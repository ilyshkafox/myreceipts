package com.githab.ilyshkafox.checkregistration.oytlay;

import com.githab.ilyshkafox.checkregistration.exceptions.ExceptionHelper;
import com.githab.ilyshkafox.checkregistration.repo.ReceiptDao;
import com.githab.ilyshkafox.checkregistration.repo.ReceiptRepository;
import com.githab.ilyshkafox.checkregistration.qrcode.QrCodeReceipt;
import com.githab.ilyshkafox.checkregistration.qrcode.QrCodeReceiptDecodeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.jooq.exception.DataAccessException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Log4j2
@RequiredArgsConstructor
public class OutlayService {
    private final ExceptionHelper exceptionHelper;
    private final QrCodeReceiptDecodeService qrService;
    private final OutlayMapper mapper;
    private final ReceiptRepository repository;

    public Mono<ReceiptDao> createReceipt(String qrcode) {
        return Mono.just(qrcode)
                .map(qrService::decode)
                .flatMap(this::save)
                .flatMap(this::sendEvent)
                .onErrorResume(e -> {
                    if (e instanceof DataAccessException) {
                        return repository.getIdByQrString(qrcode)
                                .flatMap(id -> Mono.error(exceptionHelper.createDuplicateException("Запись чека уже создана: " + qrcode + ". Id записи: " + id, "R" + id, e)));
                    }
                    return Mono.error(e);
                })
                ;
    }


    private Mono<ReceiptDao> save(QrCodeReceipt qrcode) {
        return Mono.just(qrcode)
                .map(mapper::mapResponse)
                .flatMap(repository::save)
                .doOnNext(receiptDao -> log.info("Чек сохранен: " + receiptDao));
    }
    private Mono<ReceiptDao> sendEvent(ReceiptDao qrcode) {
        return Mono.just(qrcode)
                .map(mapper::mapResponse)
                .flatMap(repository::save)
                .doOnNext(receiptDao -> log.info("Чек сохранен: " + receiptDao));
    }


//    public Mono<Outlay> createManual(Long sum, String data, LocalDate date) {
//        return null;
//    }
}
