package eu.knowledge.engine.smartconnector.edc;

import jakarta.annotation.Nullable;
import jakarta.inject.Named;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 *  We store token data in memory here.
 *  For a production grade system it makes sense to store the data in a (persistent) datastore.
 */
@Named
public class InMemoryTokenManager {

  private final List<Token> tokens = new ArrayList<>();
  private final List<TransferProcess> transferProcessInitiations = new ArrayList<>();

  public void tokenReceived(Token token) {
    tokens.add(token);
  }

  public List<Token> getTokensFor(@Nullable String participantId, @Nullable String counterPartyParticipantId) {
    if (participantId == null && counterPartyParticipantId == null) {
      return tokens;
    }

    Stream<TransferProcess> transferProcessesStream = transferProcessInitiations.stream()
      .filter((it) -> Objects.equals(it.participantId(), participantId));

    if (counterPartyParticipantId != null) {
      transferProcessesStream = transferProcessesStream.filter((it) -> Objects.equals(it.counterPartyParticipantId(), counterPartyParticipantId));
    }

    List<String> transferProcessIds = transferProcessesStream.map(TransferProcess::transferProcessResponseId).toList();

    // Get all the tokens belonging to the transferProcesses between participantId and counterPartyParticipantId
    List<Token> list = transferProcessIds.stream()
      .flatMap((transferProcessId) ->
        tokens.stream().filter((token) -> Objects.equals(transferProcessId, token.id()))
      ).toList();

    return list;
  }

  public void transferProcessInitiated(TransferProcess transferProcess) {
    transferProcessInitiations.add(transferProcess);
  }
}