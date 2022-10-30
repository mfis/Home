package de.fimatas.home.controller.model;

import de.fimatas.home.library.domain.model.ElectricVehicle;
import lombok.Data;

import java.math.BigDecimal;
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

    public BigDecimal countValue(){
        if(endVal.compareTo(maxVal)< 1){
            return maxVal.subtract(startVal).add(endVal);
        }else{
            return endVal.subtract(startVal);
        }
    }
}
