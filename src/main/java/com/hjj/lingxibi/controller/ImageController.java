package com.hjj.lingxibi.controller;

import com.hjj.lingxibi.common.BaseResponse;
import com.hjj.lingxibi.common.ErrorCode;
import com.hjj.lingxibi.common.ResultUtils;
import com.hjj.lingxibi.exception.BusinessException;
import com.hjj.lingxibi.manager.AliOSSManager;
import com.hjj.lingxibi.service.UserService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;

@RestController
@RequestMapping("/image")
public class ImageController {

    @Resource
    private AliOSSManager aliOSSManager;

    @PostMapping("/upload")
    public BaseResponse<String> uploadBlogImage(@RequestPart MultipartFile file) {
        String url = null;
        try {
            url = aliOSSManager.upload(file);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传图片失败");
        }
        return ResultUtils.success(url);
    }

}
