package dev.qrm.api.data;

import dev.qrm.api.economy.OwnerRef;
import java.util.Optional;

/** Dati dei mod collegati a un titolare, con namespace (es. "mypolicemod"). Il valore è JSON gestito dal mod. */
public interface ModDataService {
    void put(String namespace, OwnerRef owner, String key, String json);

    Optional<String> get(String namespace, OwnerRef owner, String key);

    void delete(String namespace, OwnerRef owner, String key);

    /** Rimuove tutti i dati di un namespace (mod disinstallato, su conferma dell'owner). @return righe rimosse. */
    int purgeNamespace(String namespace);
}
