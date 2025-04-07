package com.disasterrelief.intake.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import lombok.Data;

@Entity
@Table(name = "claims")
@Data
public class Claim {
    
    @Id
    private String id;
    
    @NotBlank
    private String disasterId;
    
    @NotBlank
    private String claimantName;
    
    @NotBlank
    private String claimantEmail;
    
    @NotBlank
    private String claimantPhone;
    
    @NotBlank
    private String propertyAddress;
    
    private String postalCode;
    
    @NotNull
    private Double latitude;
    
    @NotNull
    private Double longitude;
    
    @Column(length = 2000)
    private String damageDescription;
    
    @Enumerated(EnumType.STRING)
    private ClaimStatus status = ClaimStatus.SUBMITTED;
    
    private LocalDateTime submissionDate;
    
    private LocalDateTime lastUpdated;
    
    @JsonManagedReference
    @OneToMany(mappedBy = "claim", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ClaimEvidence> evidences = new ArrayList<>();
    
    public enum ClaimStatus {
        SUBMITTED,
        UNDER_REVIEW,
        ASSESSMENT_SCHEDULED,
        ASSESSMENT_COMPLETED,
        ELIGIBILITY_VERIFIED,
        PAYMENT_PROCESSING,
        PAYMENT_COMPLETED,
        DENIED,
        APPEALED
    }
    
    // Generate a unique claim ID with disaster prefix
    public static String generateClaimId(String disasterId) {
        return disasterId + "-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
