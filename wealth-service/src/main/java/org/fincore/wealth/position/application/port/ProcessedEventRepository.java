package org.fincore.wealth.position.application.port;

import java.util.UUID;

public interface ProcessedEventRepository {
    /*boolean alreadyProcessed(UUID eventId);

    void markProcessed(UUID eventId);*/


    /**
     * Enregistre atomiquement l'identifiant de l'événement.
     *
     * @return true si cet appel a réservé l'événement ;
     *         false si l'événement avait déjà été réservé/traité.
     */
    boolean claim(UUID eventId);
}