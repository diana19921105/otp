package diana.szanto.otp.model;

import lombok.Data;

@Data
public class WebShopIncomeData {

    private String webShopId;
    private int cardTransaction;
    private int transferTransaction;

    public WebShopIncomeData(String webShopId) {
        this.webShopId = webShopId;
        this.cardTransaction = 0;
        this.transferTransaction = 0;
    }

    public void addCardPayment(int amount) {
        this.cardTransaction += amount;
    }

    public void addBankTransfer(int amount) {
        this.transferTransaction += amount;
    }

    public String toCsvString() {
        return String.format("%s;%d;%d", webShopId, cardTransaction, transferTransaction);
    }

}
