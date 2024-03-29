package com.orders.model.entites;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.util.Date;

@Data
@NoArgsConstructor
@Entity
public class PlacedItems implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "createdAt")
    @CreationTimestamp
    private Date dateCreated;

    @Column(name = "last_updated")
    @UpdateTimestamp
    private Date lastUpdated;

    private String skuCode;

    private Long unitePrice;

    private Long quantity;

    @ManyToOne(cascade = {CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinColumn(name = "order_stack_id", referencedColumnName = "id")
    private OrderStack itemStack;

    @Override
    public String toString() {
        return "PlacedItems{" +
                "id=" + id +
                ", dateCreated=" + dateCreated +
                ", lastUpdated=" + lastUpdated +
                ", skuCode='" + skuCode + '\'' +
                ", unitePrice=" + unitePrice +
                ", quantity=" + quantity +
//                ", itemStack=" + itemStack + // This will cause a stack overflow error Because of the circular reference
//                 Both OrderStack and PlacedItems have a reference to each other
//                 And they are trying to print each other
                '}';
    }
}


