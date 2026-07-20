package com.problemfighter.pfspring.restapi.rr;

import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;
import com.problemfighter.pfspring.restapi.rr.response.I18nMessage;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.problemfighter.pfspring.restapi.rr.response.PageableResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

public class Utility {
    public static <E, D> PageableResponse<D> response(Page<E> page, List<D> dataList) {
        PageableResponse<D> pageableResponse = new PageableResponse<>();
        pageableResponse.data = dataList;
        Pageable pageable = page.getPageable();
        pageableResponse.addPagination(pageable.getPageNumber(), pageable.getPageSize()).setTotal(page.getTotalElements()).setTotalPage(page.getTotalPages());
        pageableResponse.success();
        return pageableResponse;
    }

    public static <E, D> PageableResponse<D> response(List<D> dataList) {
        PageableResponse<D> pageableResponse = new PageableResponse<>();
        pageableResponse.data = dataList;
        Pageable pageable = PageRequest.of(0, dataList.size());
        pageableResponse.addPagination(pageable.getPageNumber(), pageable.getPageSize()).setTotal((long) dataList.size()).setTotalPage(0);
        pageableResponse.success();
        return pageableResponse;
    }

    public static <D> DetailsResponse<D> response(D dto) {
        DetailsResponse<D> detailsResponse = new DetailsResponse<>();

        detailsResponse.data = dto;
        detailsResponse.success();

        return detailsResponse;
    }


    public static MessageResponse response(String message) {
        MessageResponse messageResponse = new MessageResponse();
        messageResponse.message = I18nMessage.message(message);
        messageResponse.code = "1200";
        return messageResponse;
    }
}
