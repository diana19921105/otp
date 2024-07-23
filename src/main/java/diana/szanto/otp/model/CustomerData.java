package diana.szanto.otp.model;

import lombok.Data;

@Data
public class CustomerData {

    private String webShopId;
    private String customerId;
    private String name;
    private String address;
    private int totalPurchases;

    public CustomerData(String webShopId, String customerId, String name, String address) {
        this.webShopId = webShopId;
        this.customerId = customerId;
        this.name = name;
        this.address = address;
        this.totalPurchases = 0;
    }

    public void addPurchase(int amount) {
        this.totalPurchases += amount;
    }

    public String toCsvString() {
        return String.format("%s;%s;%d", name, address, totalPurchases);
    }


}
