package de.fimatas.home.controller.model;

import de.fimatas.home.library.domain.model.ElectricVehicle;
import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Data
public class EvChargeDatabaseEntry {

    private LocalDateTime startTS;

    private LocalDateTime endTS;

    private Integer chargepoint;

    private ElectricVehicle electricVehicle;

    private BigDecimal startVal;

    private BigDecimal endVal;

    private BigDecimal maxVal;

    public boolean finished(){return endTS!=null;}

    public BigDecimal countValueAsKWH(){
        BigDecimal sum;
        if(maxVal.compareTo(endVal) > 0){
            sum = maxVal.subtract(startVal).add(endVal); // overflow
        }else{
            sum = endVal.subtract(startVal);
        }
        return sum.divide(new BigDecimal(1000), 4, RoundingMode.HALF_UP);
    }
}
