package testsupport

class ManageGames(driver: GameMasterDriver): Ability, GameMasterDriver by driver

val startTheGame = Interaction { actor -> actor.use<ManageGames>().startGame() }
