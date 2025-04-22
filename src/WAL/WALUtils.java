package WAL;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class WALUtils {

    public static void marcarTodosComoCommit(int bloco) {
        String logPath = switch (bloco) {
            case 1 -> "log_bloco1.txt";
            case 2 -> "log_bloco2.txt";
            case 3 -> "log_bloco3.txt";
            default -> null;
        };

        if (logPath == null) {
            System.err.println("Bloco inválido: " + bloco);
            return;
        }

        Path logFile = Path.of(logPath);
        if (!Files.exists(logFile)) {
            System.out.println("Arquivo de log não encontrado: " + logPath);
            return;
        }

        try {
            List<String> linhas = Files.readAllLines(logFile);
            List<String> atualizadas = linhas.stream()
                    .map(linha -> linha.replace(";PENDENTE", ";COMMIT"))
                    .toList();

            Files.write(logFile, atualizadas);
            System.out.println("Todas as operações do bloco " + bloco + " marcadas como COMMIT.");

        } catch (IOException e) {
            System.err.println("Erro ao atualizar o log: " + e.getMessage());
        }
    }
}
