package diana.szanto.otp.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import diana.szanto.otp.model.CustomerData;
import diana.szanto.otp.model.WebShopIncomeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportGenerationService {

    @Value("${otp.customer.path}")
    private String customerInputFile;

    @Value("${otp.out.customer.path}")
    private String customerOutputFile;

    @Value("${otp.payments.path}")
    private String transactionInputFile;

    @Value("${otp.out.payment.path}")
    private String transactionOutputFile;

    @Value("${otp.log.path}")
    private String logFile;

    @Value("${otp.out.report.path}")
    private String reportOutputFile;

    @Value("${otp.out.top-customers.path}")
    private String topCustomersFile;

    @Value("${otp.out.web-shop-report.path}")
    private String webShopReportFile;

    private final FileUtil fileUtil;

    public void generateReport() {
        try {
            fileUtil.readFile(customerInputFile, customerOutputFile, logFile);
            fileUtil.readFile(transactionInputFile, transactionOutputFile, logFile);

            Files.deleteIfExists(Paths.get(reportOutputFile));
            Files.deleteIfExists(Paths.get(topCustomersFile));
            Files.deleteIfExists(Paths.get(webShopReportFile));

            Map<String, CustomerData> customerMap = new HashMap<>();
            Map<String, WebShopIncomeData> webShopIncomeDataMap = new HashMap<>();

            fileUtil.loadCustomerData(customerMap);
            log.debug("Customer data loaded successfully.");

            fileUtil.loadTransactionData(customerMap, webShopIncomeDataMap);
            log.debug("Transaction data loaded successfully.");

            try (var writer = new BufferedWriter(new FileWriter(reportOutputFile))) {
                writer.write("NAME;ADDRESS;vásárlás összesen\n");
                for (var data : customerMap.values()) {
                    writer.write(data.toCsvString() + "\n");
                }
            }

            var topCustomers = customerMap.values().stream()
                .sorted(Comparator.comparingInt(CustomerData::getTotalPurchases).reversed())
                .limit(2)
                .toList();

            try (var writer = new BufferedWriter(new FileWriter(topCustomersFile))) {
                writer.write("NAME;ADDRESS;vásárlás összesen\n");
                for (var data : topCustomers) {
                    writer.write(data.toCsvString() + "\n");
                }
            }

            try (var writer = new BufferedWriter(new FileWriter(webShopReportFile))) {
                writer.write("WEBSHOP;kártyás vásárlások összege;átutalásos vásárlások összege\n");
                for (var income : webShopIncomeDataMap.values()) {
                    writer.write(income.toCsvString() + "\n");
                }
            }
        } catch (IOException e) {
            log.error("File manipulation failed", e);
            throw new RuntimeException(e);
        }
    }

}
