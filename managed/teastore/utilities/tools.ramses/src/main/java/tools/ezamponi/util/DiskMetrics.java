package tools.ezamponi.util;

import io.prometheus.client.CollectorRegistry;
import io.prometheus.client.Gauge;

import java.io.File;

public class DiskMetrics {

    private final Gauge diskTotalBytes;
    private final Gauge diskFreeBytes;

    public DiskMetrics() {

        // Inizializza il registro delle metriche
        CollectorRegistry prometheusRegistry = CollectorRegistry.defaultRegistry;

        // Registra le metriche custom per lo spazio su disco
        diskTotalBytes = Gauge.build()
                .name("disk_total_bytes")
                .help("Total disk space in bytes.")
                .register(prometheusRegistry);

        diskFreeBytes = Gauge.build()
                .name("disk_free_bytes")
                .help("Free disk space in bytes.")
                .register(prometheusRegistry);
    }

    // Metodo per aggiornare le metriche dello spazio su disco
    public void updateDiskMetrics() {
        File root = new File("/");
        long totalSpace = root.getTotalSpace();
        long freeSpace = root.getFreeSpace();

        // Aggiorna i valori delle metriche
        diskTotalBytes.set(totalSpace);
        diskFreeBytes.set(freeSpace);
    }

    public static void main(String[] args) {
        DiskMetrics diskMetrics = new DiskMetrics();

        // Esempio di aggiornamento delle metriche (puoi chiamare questo metodo periodicamente)
        diskMetrics.updateDiskMetrics();
        //System.out.println("TOTAL BYTES: " + diskMetrics.diskTotalBytes.get());
        //System.out.println("FREE BYTES: " + diskMetrics.diskFreeBytes.get());
    }
}
