package com.plovdiv.advisor.web;

import com.plovdiv.advisor.dto.BuyerProfile;
import com.plovdiv.advisor.dto.ConstructionType;
import com.plovdiv.advisor.dto.District;
import com.plovdiv.advisor.dto.HeatingType;
import com.plovdiv.advisor.dto.SearchCriteria;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SearchForm {
    private BuyerProfile profile = BuyerProfile.FAMILY;
    private BigDecimal maxBudgetEUR = new BigDecimal("150000");
    private List<District> districts = new ArrayList<>();
    private int minRooms = 2;
    private int minBedrooms = 1;
    private ConstructionType constructionType;
    private HeatingType heatingType;
    private boolean requiresElevator;
    private boolean requiresParking;
    private boolean requiresBalcony;
    private List<String> priorities = new ArrayList<>();

    public SearchCriteria toCriteria() {
        return new SearchCriteria(
                profile,
                maxBudgetEUR,
                districts == null ? List.of() : districts,
                Math.max(0, minRooms),
                Math.max(0, minBedrooms),
                constructionType,
                heatingType,
                requiresElevator,
                requiresParking,
                requiresBalcony,
                priorities == null ? List.of() : priorities
        );
    }

}
