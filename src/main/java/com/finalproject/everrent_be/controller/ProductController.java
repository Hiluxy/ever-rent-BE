package com.finalproject.everrent_be.controller;

import com.finalproject.everrent_be.dto.ProductRequestDto;
import com.finalproject.everrent_be.dto.ResponseDto;
import com.finalproject.everrent_be.service.ProductService;
import io.lettuce.core.dynamic.annotation.Param;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    //메인페이지
    @GetMapping("/products")
    public ResponseDto<?> getAllProduct(){
        return productService.getAllProduct();
    }


    //상세페이지 조회
    @GetMapping("/products/{productId}")
    public ResponseDto<?> getProduct(@PathVariable Long productId){
        return productService.getProduct(productId);
    }

    //카테고리 별 분류
    @GetMapping("/categories/{categoryId}")
    public ResponseDto<?> getFromCategory(@PathVariable String categoryId)
    {
        return productService.getFromCategory(categoryId);

    }

    //상세페이지 작성
    @PostMapping("/products")
    public ResponseDto<?> createProduct(@RequestPart MultipartFile multipartFile, @RequestPart ProductRequestDto requestDto,
                                     HttpServletRequest request) {
        return productService.createProduct(multipartFile, requestDto, request);
    }

    //상세페이지 수정
    @PutMapping("/products/{productId}")
    public ResponseDto<?> updateProduct(@PathVariable Long productId,@RequestPart MultipartFile multipartFile, @RequestPart ProductRequestDto requestDto, HttpServletRequest request){
        return productService.updateProduct(productId,multipartFile,requestDto,request);
    }


    //상세페이지 삭제
    @DeleteMapping("/products/{productId}")
    public ResponseDto<?> deleteProduct(@PathVariable Long productId,HttpServletRequest request){
        return productService.deleteProduct(productId,request);
    }






}
