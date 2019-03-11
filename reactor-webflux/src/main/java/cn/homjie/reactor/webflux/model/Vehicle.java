package cn.homjie.reactor.webflux.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jiehong.jh
 * @date 2019-03-08
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle implements Serializable {

    private static final long serialVersionUID = 4435529730640422204L;

    private int id;
    private String carPlateNumber;
    private Long weight;
    private Integer speed;
    private String color;
    private Integer modelYear;
    private String gasType;
}
