package com.products.products.repository;

import com.products.products.model.entitis.CoreItem;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoreItemRepo extends MongoRepository<CoreItem, String> {


}
