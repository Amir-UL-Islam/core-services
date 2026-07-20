package com.central.security.core.urls.service;

import com.central.security.events.BeforeDeletePrivilege;
import com.problemfighter.pfspring.restapi.rr.request.RequestData;
import com.problemfighter.pfspring.restapi.rr.response.DetailsResponse;
import com.problemfighter.pfspring.restapi.rr.response.MessageResponse;
import com.problemfighter.pfspring.restapi.rr.response.PageableResponse;
import com.central.security.core.urls.repository.UrlsRepository;
import com.central.security.core.urls.model.dto.UrlDTO;
import com.central.security.core.urls.model.entity.Url;
import com.problemfighter.pfspring.restapi.rr.RequestResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class UrlService implements RequestResponse {

    private final UrlsRepository urlsRepository;

    public PageableResponse<UrlDTO> findAll(final String filter, final Pageable pageable) {
        Page<Url> page;
        if (filter != null) {
            Long longFilter = null;
            try {
                longFilter = Long.parseLong(filter);
            } catch (final NumberFormatException numberFormatException) {
                // keep null - no parseable input
            }
            page = urlsRepository.findAllById(longFilter, pageable);
        } else {
            page = urlsRepository.findAll(pageable);
        }
        return responseProcessor().response(page, UrlDTO.class);
    }

    public DetailsResponse<UrlDTO> get(final Long id) {
        return responseProcessor().response(urlsRepository.findById(id), UrlDTO.class);
    }

    public DetailsResponse<UrlDTO> getByPrivilege(final Long id) {
        return responseProcessor().response(urlsRepository.findAllByPrivilegeId(id), UrlDTO.class);
    }

    public MessageResponse create(final RequestData<UrlDTO> urlsDTO) {
        Url url = new Url();
        requestProcessor().process(urlsDTO, url);
        url = urlsRepository.save(url);
        return responseProcessor().response("URL created with ID: " + url.getId());
    }

    public MessageResponse update(final RequestData<UrlDTO> data) {
        requestProcessor().validateId(data.getData().getId(), "Url ID can't be null");
        Url entity = dataUtil().validateAndOptionToEntity(urlsRepository.findById(data.getData().getId()), "Url not found with ID: " + data.getData().getId());

        requestProcessor().process(data, entity);
        entity = urlsRepository.save(entity);
        return responseProcessor().response("URL updated with ID: " + entity.getId());
    }

    public MessageResponse delete(final Long id) {
        Url url = dataUtil().validateAndOptionToEntity(urlsRepository.findById(id), "URL not found with ID: " + id);
        // Remove this URL from all privileges that reference it (owning side)
        url.getPrivileges().forEach(privilege -> privilege.getUrls().remove(url));
        urlsRepository.delete(url);
        return responseProcessor().response("URL deleted with ID: " + id);
    }


    public boolean endpointExists(final String endpoint) {
        return urlsRepository.existsByEndpointIgnoreCase(endpoint);
    }

    /**
     * True when the composite (endpoint, method) pair already exists in the database.
     * Mirrors the {@code uk_endpoint_method} uniqueness constraint.
     */
    public boolean endpointAndMethodExists(final String endpoint, final String method) {
        return urlsRepository.existsByEndpointIgnoreCaseAndMethodIgnoreCase(endpoint, method);
    }

    @EventListener(BeforeDeletePrivilege.class)
    public void on(final BeforeDeletePrivilege event) {
        // remove many-to-many relations at owning side
        urlsRepository.findAllByPrivilegeId(event.getId()).forEach(role ->
                role.getPrivileges().removeIf(privilege -> privilege.getId().equals(event.getId())));
    }

}
