package com.orders.model.entites;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Entity
public class OrderStack implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Column(name = "createdAt")
    @CreationTimestamp
    private Date dateCreated;

    @Column(name = "last_updated")
    @UpdateTimestamp
    private Date lastUpdated;


    private String orderNumber;

    @OneToMany(
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            targetEntity = PlacedItems.class,
            fetch = FetchType.EAGER,
            mappedBy = "itemStack" // The mappedBy value should refer to property name of the owner class.
            // In this case the property name of the Orders class is item[private Orders item;]
    )
    @OrderBy("dateCreated ASC")
    private List<PlacedItems> orderItems = new ArrayList<>();
}
