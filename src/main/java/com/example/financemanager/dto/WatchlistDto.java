package com.example.financemanager.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WatchlistDto {
    private Long id;
    private String symbol;
    private String name;
    private String type;
    private String notes;
    private BigDecimal investedAmount;
    private BigDecimal totalAmount;
    private Integer transactionCount;
    private BigDecimal averagePrice;

    private LocalDateTime createdAt;

    // Standard-Konstruktor
    public WatchlistDto() {
        this.investedAmount = BigDecimal.ZERO;
        this.totalAmount = BigDecimal.ZERO;
        this.transactionCount = 0;
    }

    // Alle Getters und Setters...
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSymbol() { return symbol; }
    public void setSymbol(String symbol) { this.symbol = symbol; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public void setNotes(String notes) { this.notes = notes; }

    public BigDecimal getInvestedAmount() { return investedAmount; }
    public void setInvestedAmount(BigDecimal investedAmount) {
        this.investedAmount = investedAmount != null ? investedAmount : BigDecimal.ZERO;
    }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount != null ? totalAmount : BigDecimal.ZERO;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount != null ? transactionCount : 0;
    }

    public void setAveragePrice(BigDecimal averagePrice) {
        this.averagePrice = averagePrice != null ? averagePrice : BigDecimal.ZERO;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "WatchlistDto{" +
                "id=" + id +
                ", symbol='" + symbol + '\'' +
                ", name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", notes='" + notes + '\'' +
                ", investedAmount=" + investedAmount +
                ", totalAmount=" + totalAmount +
                ", transactionCount=" + transactionCount +
                ", createdAt=" + createdAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        WatchlistDto that = (WatchlistDto) o;

        if (id != null ? !id.equals(that.id) : that.id != null) return false;
        if (symbol != null ? !symbol.equals(that.symbol) : that.symbol != null) return false;
        return type != null ? type.equals(that.type) : that.type == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (symbol != null ? symbol.hashCode() : 0);
        result = 31 * result + (type != null ? type.hashCode() : 0);
        return result;
    }
}
