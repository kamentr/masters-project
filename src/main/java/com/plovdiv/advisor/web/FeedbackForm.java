package com.plovdiv.advisor.web;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackForm {
    private int rating = 5;
    private String comment = "";
    private boolean useful = true;
}
