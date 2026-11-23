package sabaylakbay_1.model;

import java.time.LocalDate;

/** Search, filter and sort options from the Home page. Null / 0 means 'not used'. */
public class GroupFilter {

    private String keyword;
    private LocalDate dateFrom;
    private LocalDate dateTo;
    private Double maxBudget;
    private int minSlots;
    private SortOption sort;


    public GroupFilter() { sort = SortOption.NEWEST; }

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }
    public LocalDate getDateTo() { return dateTo; }
    public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }
    public Double getMaxBudget() { return maxBudget; }
    public void setMaxBudget(Double maxBudget) { this.maxBudget = maxBudget; }
    public int getMinSlots() { return minSlots; }
    public void setMinSlots(int minSlots) { this.minSlots = minSlots; }
    public SortOption getSort() { return sort; }
    public void setSort(SortOption sort) { this.sort = sort; }
}
