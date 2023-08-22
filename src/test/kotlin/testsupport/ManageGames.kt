package testsupport

class ManageGames(driver: GameMasterDriver): Ability, GameMasterDriver by driver

val startTheGame = Interaction { actor -> actor.use<ManageGames>().startGame() }

val startTheTrickTakingPhase = Interaction { actor -> actor.use<ManageGames>().startTrickTaking() }
