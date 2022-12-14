package com.finalproject.everrent_be.service;



import com.finalproject.everrent_be.dto.*;
import com.finalproject.everrent_be.exception.ErrorCode;
import com.finalproject.everrent_be.jwt.TokenProvider;
import com.finalproject.everrent_be.model.Member;
import com.finalproject.everrent_be.model.RefreshToken;
import com.finalproject.everrent_be.repository.MemberRepository;
import com.finalproject.everrent_be.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final AuthenticationManagerBuilder authenticationManagerBuilder;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;



    public boolean nicknameCheck(MemberCheckRequestDto checkRequestDto)
    {
        if(memberRepository.existsByMemberName(checkRequestDto.getMemberName()))
        {
            return false;
        }
        return true;
    }

    public boolean emailCheck(EmailCheckRequestDto checkRequestDto)
    {
        if(memberRepository.existsByEmail(checkRequestDto.getEmail()))
        {
            return false;
        }
        return true;
    }

    @Transactional
    public ResponseDto signup(MemberRequestDto memberRequestDto) {


        if(memberRepository.existsByMemberName(memberRequestDto.getMemberName()))
        {
            return ResponseDto.is_Fail(ErrorCode.DUPLICATE_NICKNAME);
        }
        if(memberRepository.existsByEmail(memberRequestDto.getEmail()))
        {
            return ResponseDto.is_Fail(ErrorCode.DUPLICATE_EMAIL);
        }
        if(!(Pattern.matches("[a-zA-Z0-9]*$",memberRequestDto.getMemberName()) && (memberRequestDto.getMemberName().length() > 1 && memberRequestDto.getMemberName().length() <15)
                && Pattern.matches("[a-zA-Z0-9]*$",memberRequestDto.getPassword()) && (memberRequestDto.getPassword().length() > 7 && memberRequestDto.getPassword().length() <33))){

            throw new IllegalArgumentException("????????? ?????? ???????????? ????????? ??????????????????.");
        }
        Member member = memberRequestDto.toMember(passwordEncoder);
        memberRepository.save(member);

        return ResponseDto.is_Success(member);
    }


    @Transactional
    public TokenDto login(LoginRequestDto loginRequestDto) {
//        if (!memberRepository.existsByNickname(memberRequestDto.getNickname()) ||
//                !memberRepository.existsByPassword(passwordEncoder.encode(memberRequestDto.getPassword()))) {
//            throw new RuntimeException("???????????? ?????? ??? ????????????");
//        }
        // 1. Login ID/PW ??? ???????????? AuthenticationToken ??????
        UsernamePasswordAuthenticationToken authenticationToken = loginRequestDto.toAuthentication();
        // 2. ????????? ?????? (????????? ???????????? ??????) ??? ??????????????? ??????
        //    authenticate ???????????? ????????? ??? ??? CustomUserDetailsService ?????? ???????????? loadUserByUsername ???????????? ?????????
        /*    AuthService?????? AuthenticationManagerBuilder ?????? ??????
              AuthenticationManagerBuilder ?????? AuthenticationManager ??? ????????? ProviderManager ??????
              org.springframework.security.authentication.ProviderManager ??? AbstractUserDetailsAuthenticationProvider ??? ?????? ???????????? DaoAuthenticationProvider ??? ??????????????? ??????
              DaoAuthenticationProvider ??? authenticate ????????? retrieveUser ??? DB ??? ?????? ????????? ????????? ???????????? additionalAuthenticationChecks ??? ???????????? ??????
              retrieveUser ???????????? UserDetailsService ?????????????????? ?????? ????????? CustomUserDetailsService ???????????? ??????????????? ???????????? loadUserByUsername ??? ?????????*/

        try{
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

            // 3. ?????? ????????? ???????????? JWT ?????? ??????
            TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);
            // 4. RefreshToken ??????
            RefreshToken refreshToken = RefreshToken.builder()
                    .key(authentication.getName())
                    .value(tokenDto.getRefreshToken())
                    .build();
            refreshTokenRepository.save(refreshToken);
            // 5. ?????? ??????
            return tokenDto;
        } catch (Exception e){
            throw new IllegalArgumentException("???????????? ?????? ??? ????????????.");
        }
    }


    @Transactional
    public TokenDto reissue(TokenRequestDto tokenRequestDto) {
        // 1. Refresh Token ??????
        if (!tokenProvider.validateToken(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("Refresh Token ??? ???????????? ????????????.");
        }

        // 2. Access Token ?????? Member ID ????????????
        Authentication authentication = tokenProvider.getAuthentication(tokenRequestDto.getAccessToken());

        // 3. ??????????????? Member ID ??? ???????????? Refresh Token ??? ?????????
        RefreshToken refreshToken = refreshTokenRepository.findByKkey(authentication.getName())
                .orElseThrow(() -> new RuntimeException("???????????? ??? ??????????????????."));

        // 4. Refresh Token ??????????????? ??????
        if (!refreshToken.getVvalue().equals(tokenRequestDto.getRefreshToken())) {
            throw new RuntimeException("????????? ?????? ????????? ???????????? ????????????.");
        }

        // 5. ????????? ?????? ??????
        TokenDto tokenDto = tokenProvider.generateTokenDto(authentication);

        // 6. ????????? ?????? ????????????
        RefreshToken newRefreshToken = refreshToken.updateValue(tokenDto.getRefreshToken());
        refreshTokenRepository.save(newRefreshToken);

        // ?????? ??????
        return tokenDto;
    }
}