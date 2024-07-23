package diana.szanto.otp.controller;

import diana.szanto.otp.api.OtpApi;
import diana.szanto.otp.service.ReportGenerationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class OtpController implements OtpApi {

    private final ReportGenerationService reportGenerationService;

    @Override
    public ResponseEntity<Void> generateReport() {
        reportGenerationService.generateReport();
        return ResponseEntity.noContent().build();
    }

}
