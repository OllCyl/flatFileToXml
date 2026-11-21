package elsewise.portfolio.cylven.datamapping;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class FlatFileConverterService {

    private final FlatFileReaderService flatFileReaderService;

    public FlatFileConverterService(FlatFileReaderService flatFileReaderService) {
        this.flatFileReaderService = flatFileReaderService;
    }

    @PostConstruct
    public void printFileContent() {
        String content = flatFileReaderService.getFileContent();

    }
}
