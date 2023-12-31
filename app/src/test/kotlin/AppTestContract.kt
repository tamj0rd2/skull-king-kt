import TestHelpers.playUpTo
import TestHelpers.playUpToStartOf
import TestHelpers.skipToTrickTaking
import com.tamj0rd2.domain.*
import com.tamj0rd2.domain.Card.SpecialCard.Companion.mermaid
import com.tamj0rd2.domain.DisplayBid.*
import com.tamj0rd2.domain.GameErrorCode.*
import com.tamj0rd2.domain.GamePhase.InProgress
import com.tamj0rd2.domain.RoundPhase.*
import org.junit.jupiter.api.Nested
import strikt.assertions.atLeast
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.isEmpty
import testsupport.*
import testsupport.annotations.AutomatedGameMasterTests
import testsupport.annotations.UnhappyPath
import testsupport.annotations.Wip
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration

interface AbilityFactory {
    fun participateInGames(): ParticipateInGames
    fun manageGames(): ManageGames
}

interface TestConfiguration : AbilityFactory {
    fun setup()
    fun teardown()
    val automaticGameMasterDelay: Duration
    fun automateGameMasterCommands() {
        TODO("automateGameMasterCommands not implemented")
    }
}

// things for the future:
// TODO: turn order
// TODO: scoring

abstract class AppTestContract(private val testConfiguration: TestConfiguration) : Ensurer by ensurer(),
    AbilityFactory by testConfiguration {
    private val freddy by lazy { Actor("Freddy First").whoCan(participateInGames()) }
    private val sally by lazy { Actor("Sally Second").whoCan(participateInGames()) }

    @BeforeTest
    fun setup() = testConfiguration.setup()

    @AfterTest
    fun teardown() = testConfiguration.teardown()

    @Nested
    @Suppress("unused")
    inner class GivenThereIsAManualGameMaster {

        private val gary by lazy { Actor("Gary GameMaster").whoCan(manageGames()) }

        @Test
        fun `sitting at an empty table and waiting for more players to join`() {
            freddy(
                SitsAtTheTable,
                ensures {
                    that(ThePlayersAtTheTable) { are(freddy) }
                    that(TheGameState) { Is(GamePhase.WaitingForMorePlayers) }
                },
            )
        }

        @Test
        fun `waiting for sally to bid`() {
            freddy and sally both SitAtTheTable
            gary(SaysTheGameCanStart)
            freddy and sally both ensureThat(TheRoundPhase) { Is(Bidding) }
            freddy(Bids(1))
            freddy and sally both ensure {
                that(TheRoundPhase) { Is(Bidding) }
                // TODO: this seems odd. The player can't see their own bid unless everyone has bid...
                that(TheBidsTheySee) { areFor(freddy.whoseBidIsHidden(), sally.whoHasNotBid()) }
            }
        }

        @Test
        @Wip
        fun `playing a card and waiting for the next player to do the same`() {
            freddy and sally both SitAtTheTable
            gary(
                RigsTheDeck.SoThat(freddy).willEndUpWith(11.blue),
                RigsTheDeck.SoThat(sally).willEndUpWith(11.black),
                SaysTheGameCanStart
            )
            freddy and sally both Bid(1)
            freddy and sally both ensureThat(TheRoundPhase) { Is(BiddingCompleted) }

            gary(SaysTheTrickCanStart)
            freddy and sally both ensure {
                that(TheRoundPhase) { Is(TrickTaking) }
                that(TheCurrentPlayer) { Is(freddy) }
            }

            freddy(
                ensuresThat(HisHand) { hasSize(1) },
                Plays(11.blue),
                ensuresThat(HisHand) { isEmpty() },
            )

            freddy and sally both ensure {
                that(TheCurrentTrick) { cardsAre(11.blue.playedBy(freddy)) }
                that(TheRoundPhase) { Is(TrickTaking) }
                that(TheCurrentPlayer) { Is(sally) }
            }
        }

        @Test
        @Wip
        fun `winning a trick`() {
            freddy and sally both SitAtTheTable
            gary(
                RigsTheDeck.SoThat(freddy).willEndUpWith(11.blue),
                RigsTheDeck.SoThat(sally).willEndUpWith(12.blue),
                SaysTheGameCanStart
            )
            freddy and sally both Bid(1)
            freddy and sally both ensureThat(TheRoundPhase) { Is(BiddingCompleted) }

            gary(SaysTheTrickCanStart)
            freddy and sally both ensure {
                that(TheRoundPhase) { Is(TrickTaking) }
                that(TheCurrentPlayer) { Is(freddy) }
            }

            freddy and sally both Play.theirFirstPlayableCard

            freddy and sally both ensure {
                that(TheRoundPhase) { Is(TrickCompleted) }
                that(TheWinnerOfTheTrick) { Is(sally) }
                that(TheWinsOfTheRound) { are(freddy won 0, sally won 1) }
            }

            // testing for round 2
            gary(
                RigsTheDeck.SoThat(freddy).willEndUpWith(11.blue, 12.blue),
                RigsTheDeck.SoThat(sally).willEndUpWith(1.blue, 2.blue),
            )
            skipToTrickTaking(theGameMaster = gary, thePlayers = listOf(freddy, sally))

            freddy and sally both ensure {
                that(TheRoundNumber) { Is(RoundNumber.of(2)) }
                that(TheRoundPhase) { Is(TrickTaking) }
                that(TheCurrentPlayer) { Is(freddy) }
            }

            freddy and sally both Play.theirFirstPlayableCard
            gary(SaysTheNextTrickCanStart)
            freddy and sally both Play.theirFirstPlayableCard

            freddy and sally both ensure {
                that(TheRoundPhase) { Is(TrickCompleted) }
                that(TheWinsOfTheRound) { are(freddy won 2, sally won 0) }
            }
        }

        @UnhappyPath
        @Test
        @Wip
        fun `cannot play a card when the trick is not in progress`() {
            freddy and sally both SitAtTheTable
            gary(SaysTheGameCanStart)

            freddy(Bids(1))
            freddy and sally both ensure {
                that(TheRoundPhase) { Is(Bidding) }
                that(TheirHand) { onlyContainsCardsThatAreNotPlayable() }
            }

            sally(Bids(1))
            freddy and sally both ensure {
                that(TheRoundPhase) { Is(BiddingCompleted) }
                that(TheirHand) { onlyContainsCardsThatAreNotPlayable() }
            }

            gary(SaysTheTrickCanStart)
            freddy(ensuresThat(HisHand) { onlyContainsCardsThatArePlayable() })
            sally(ensuresThat(HerHand) { onlyContainsCardsThatAreNotPlayable() })
        }

        @UnhappyPath
        @Test
        @Wip
        fun `cannot play a card that would break suit rules`() {
            val thePlayers = listOf(freddy, sally)
            val theGameMaster = gary

            playUpTo(endOfRound = RoundNumber.of(1), theGameMaster = theGameMaster, thePlayers = thePlayers)

            theGameMaster(
                RigsTheDeck.SoThat(freddy).willEndUpWith(1.blue, 2.blue),
                RigsTheDeck.SoThat(sally).willEndUpWith(3.blue, 4.red),
            )
            skipToTrickTaking(theGameMaster = theGameMaster, thePlayers = thePlayers)

            freddy(Plays(1.blue))
            sally(Playing(4.red) wouldFailBecause PlayingCardWouldBreakSuitRules)

            // recovery
            sally(ensuresThat(HerHand) { sizeIs(2) })
            sally(Plays(3.blue))

            theGameMaster(SaysTheNextTrickCanStart)
            freddy(Plays(2.blue))
            sally(Plays(4.red))
        }

        @Test
        @Wip
        fun `can play special cards when you have a card for the correct suit - sanity check`() {
            val thePlayers = listOf(freddy, sally)
            val theGameMaster = gary

            playUpTo(endOfRound = RoundNumber.of(1), theGameMaster = theGameMaster, thePlayers = thePlayers)

            theGameMaster(
                RigsTheDeck.SoThat(freddy).willEndUpWith(1.blue, 2.blue),
                RigsTheDeck.SoThat(sally).willEndUpWith(3.blue, Card.SpecialCard.escape),
            )
            skipToTrickTaking(theGameMaster = theGameMaster, thePlayers = thePlayers)

            freddy(Plays(1.blue))
            sally(Plays(Card.SpecialCard.escape))
            thePlayers each ensureThat(TheRoundPhase) { Is(TrickCompleted) }
        }

        @UnhappyPath
        @Test
        @Wip
        fun `cannot play a card when it is not their turn`() {
            val thirzah = Actor("Thirzah Third").whoCan(participateInGames())
            val thePlayers = listOf(freddy, sally, thirzah)
            playUpToStartOf(RoundNumber.of(2), trick = TrickNumber.of(1), theGameMaster = gary, thePlayers = thePlayers)

            thePlayers each ensureThat(TheCurrentPlayer) { Is(freddy) }
            freddy(ensuresThat(HisHand) { onlyContainsCardsThatArePlayable() })
            sally and thirzah both ensureThat(TheirHand) { onlyContainsCardsThatAreNotPlayable() }

            freddy(Plays.theirFirstPlayableCard)
            thePlayers each ensureThat(TheCurrentPlayer) { Is(sally) }
            sally(ensuresThat(HerHand) { atLeast(1) { isAPlayableCard() } })
            freddy and thirzah both ensureThat(TheirHand) { onlyContainsCardsThatAreNotPlayable() }

            sally(Plays.theirFirstPlayableCard)
            thePlayers each ensureThat(TheCurrentPlayer) { Is(thirzah) }
            thirzah(ensuresThat(HerHand) { containsAtLeast1PlayableCard() })
            freddy and sally both ensureThat(TheirHand) { onlyContainsCardsThatAreNotPlayable() }

            thirzah(Plays.theirFirstPlayableCard)
            thePlayers each ensureThat(TheirHand) { onlyContainsCardsThatAreNotPlayable() }
        }

        @UnhappyPath
        @Test
        @Wip
        fun `cannot bid before the game has started`() {
            freddy and sally both SitAtTheTable
            freddy(Bidding(1) wouldFailBecause GameNotInProgress)
            freddy and sally both ensureThat(TheGameState) { Is(GamePhase.WaitingToStart) }
        }

        @UnhappyPath
        @Test
        @Wip
        fun `sanity check - suit rules`() {
            // I want to make sure that the `firstPlayableCard` that is suggested, is actually playable
            val thePlayers = listOf(freddy, sally)
            playUpTo(
                endOfRound = RoundNumber.of(1),
                theGameMaster = gary,
                thePlayers = thePlayers
            )

            gary(
                RigsTheDeck.SoThat(freddy).willEndUpWith(10.red, mermaid),
                RigsTheDeck.SoThat(sally).willEndUpWith(8.black, 12.red),
                SaysTheRoundCanStart
            )

            thePlayers each Bid(2)
            gary(SaysTheTrickCanStart)
            freddy(Plays(10.red))

            sally(ensuresThat(HerHand) { containsExactly(8.black.notPlayable(), 12.red.playable()) })
            sally(Playing(8.black) wouldFailBecause PlayingCardWouldBreakSuitRules)
        }

        @UnhappyPath
        @Test
        @Wip
        fun `cannot bid outside of the bidding phase`() {
            freddy and sally both SitAtTheTable
            gary(SaysTheGameCanStart)
            freddy and sally both Bid(1)
            freddy and sally both ensureThat(TheRoundPhase) { Is(BiddingCompleted) }
            gary(SaysTheTrickCanStart)
            freddy and sally both ensureThat(TheRoundPhase) { Is(TrickTaking) }
            freddy(Bidding(1) wouldFailBecause BiddingIsNotInProgress)
        }

        @UnhappyPath
        @Test
        @Wip
        fun `cannot bid twice`() {
            freddy and sally both SitAtTheTable
            gary(SaysTheGameCanStart)
            freddy(Bids(1))
            freddy(Bidding(1) wouldFailBecause AlreadyPlacedABid)
        }

        @UnhappyPath
        @Test
        @Wip
        fun `cannot bid more than the current round number`() {
            freddy and sally both SitAtTheTable
            gary(SaysTheGameCanStart)
            freddy(Bidding(2) wouldFailBecause BidLessThan0OrGreaterThanRoundNumber)
        }

        @UnhappyPath
        @Test
        @Wip
        fun `a player can't join twice`() {
            freddy(SitsAtTheTable)
            val freddyOnASecondDevice = Actor(freddy.name).whoCan(participateInGames())
            freddyOnASecondDevice(SittingAtTheTable wouldFailBecause PlayerWithSameNameAlreadyInGame)
        }

        @Test
        @Wip
        fun `playing a game from start to finish`() {
            freddy and sally both SitAtTheTable
            freddy and sally both ensure {
                that(ThePlayersAtTheTable) { are(freddy, sally) }
                that(TheGameState) { Is(GamePhase.WaitingToStart) }
            }

            // round 1
            gary(SaysTheGameCanStart)
            freddy and sally both ensure {
                that(TheRoundNumber) { Is(RoundNumber.of(1)) }
                that(TheirHand) { hasSize(1) }
                that(TheRoundPhase) { Is(Bidding) }
            }

            // round 1 bidding
            freddy and sally both Bid(1)
            freddy and sally both ensure {
                that(TheBidsTheySee) { areFor(freddy whoBid 1, sally whoBid 1) }
                that(TheRoundPhase) { Is(BiddingCompleted) }
            }

            // round 1 trick taking
            gary(SaysTheTrickCanStart)
            freddy and sally both ensure {
                that(TheRoundPhase) { Is(TrickTaking) }
                that(TheTrickNumber) { Is(TrickNumber.of(1)) }
                that(TheCurrentPlayer) { Is(freddy.playerId) }
            }

            val freddysCard = freddy.asksAbout(TheirFirstCard).also { freddy(Plays(it)) }
            val sallysCard = sally.asksAbout(TheirFirstCard).also { sally(Plays(it)) }
            freddy and sally both ensure {
                that(TheCurrentTrick) { cardsAre(freddysCard.playedBy(freddy), sallysCard.playedBy(sally)) }
                that(TheRoundPhase) { Is(TrickCompleted) }
                that(TheirHand) { isEmpty() }
            }

            // round 2
            gary(SaysTheRoundCanStart)
            freddy and sally both ensure {
                that(TheRoundNumber) { Is(RoundNumber.of(2)) }
                that(TheirHand) { hasSize(2) }
                that(TheRoundPhase) { Is(Bidding) }
            }

            // round 2 bidding
            freddy and sally both Bid(2)
            freddy and sally both ensure {
                that(TheBidsTheySee) { areFor(freddy whoBid 2, sally whoBid 2) }
                that(TheRoundPhase) { Is(BiddingCompleted) }
            }

            // round 2 trick 1
            gary(SaysTheTrickCanStart)
            freddy and sally both ensure {
                that(TheRoundPhase) { Is(TrickTaking) }
                that(TheTrickNumber) { Is(TrickNumber.of(1)) }
                that(TheCurrentPlayer) { Is(freddy.playerId) }
            }
            freddy and sally both Play.theirFirstPlayableCard
            freddy and sally both ensure {
                that(TheCurrentTrick) { hasCardCount(2) }
                that(TheRoundPhase) { Is(TrickCompleted) }
                that(TheirHand) { sizeIs(1) }
            }

            // round 2 trick 2
            gary(SaysTheTrickCanStart)
            freddy and sally both ensure {
                that(TheTrickNumber) { Is(TrickNumber.of(2)) }
                that(TheCurrentPlayer) { Is(freddy.playerId) }
            }

            freddy and sally both Play.theirFirstPlayableCard
            freddy and sally both ensure {
                that(TheCurrentTrick) { hasCardCount(2) }
                that(TheRoundPhase) { Is(TrickCompleted) }
                that(TheirHand) { isEmpty() }
            }

            // rounds 3 - 10
            (3..10).forEach { roundNumber ->
                // round X
                gary(SaysTheRoundCanStart)
                freddy and sally both ensure {
                    that(TheRoundNumber) { Is(RoundNumber.of(roundNumber)) }
                    that(TheirHand) { hasSize(roundNumber) }
                }

                // round X bidding
                freddy and sally both Bid(roundNumber)
                freddy and sally both ensure {
                    that(TheBidsTheySee) { areFor(freddy whoBid roundNumber, sally whoBid roundNumber) }
                    that(TheRoundPhase) { Is(BiddingCompleted) }
                }

                // round X trick 1-X
                (1..roundNumber).forEach { trickNumber ->
                    gary(SaysTheTrickCanStart)
                    freddy and sally both ensure {
                        that(TheRoundPhase) { Is(TrickTaking) }
                        that(TheTrickNumber) { Is(TrickNumber.of(trickNumber)) }
                        that(TheirHand) { hasSize(roundNumber - trickNumber + 1) }
                        that(TheCurrentPlayer) { Is(freddy) }
                    }

                    freddy and sally both Play.theirFirstPlayableCard
                    freddy and sally both ensure {
                        that(TheCurrentTrick) { hasCardCount(2) }
                        that(TheRoundPhase) { Is(TrickCompleted) }
                        that(TheirHand) { sizeIs(roundNumber - trickNumber) }
                    }
                }
            }

            freddy and sally both ensure {
                that(TheRoundNumber) { Is(RoundNumber.of(10)) }
                that(TheTrickNumber) { Is(TrickNumber.of(10)) }
                that(TheirHand) { isEmpty() }
                that(TheGameState) { Is(GamePhase.Complete) }
            }
        }
    }

    @Wip
    @Nested
    @Suppress("unused")
    @AutomatedGameMasterTests
    inner class GivenAutomatedGameMasterCommandsAreEnabled {
        init {
            testConfiguration.automateGameMasterCommands()
        }

        private val gmDelay = testConfiguration.automaticGameMasterDelay
        private val withinTimeout = gmDelay * 2

        @Test
        fun `the game automatically starts after a delay when the minimum table size is reached`() {
            freddy and sally both SitAtTheTable
            // TODO: why isn't contains exactly typesafe?
            freddy and sally both ensureThat(ThePlayersAtTheTable) { are(freddy, sally) }
            freddy and sally both ensure(withinTimeout) {
                that(TheGameState) { Is(InProgress) }
                that(TheRoundNumber) { Is(RoundNumber.of(1)) }
                that(TheirHand) { hasSize(1) }
                that(TheRoundPhase) { Is(Bidding) }
            }
        }

        @Test
        fun `the auto-start of the game still allows for other people to join`() {
            val thirzah = Actor("Thirzah Third").whoCan(participateInGames())

            freddy and sally both SitAtTheTable
            Thread.sleep((gmDelay / 4).inWholeMilliseconds)
            thirzah(SitsAtTheTable)

            freddy and sally and thirzah each ensureThat(ThePlayersAtTheTable) { are(freddy, sally, thirzah) }
            freddy and sally and thirzah each ensure(withinTimeout) {
                that(TheGameState) { Is(InProgress) }
                that(TheRoundNumber) { Is(RoundNumber.of(1)) }
                that(TheirHand) { hasSize(1) }
                that(TheRoundPhase) { Is(Bidding) }
            }
        }

        @Test
        fun `when all players have bid, the trick automatically begins after a delay`() {
            freddy and sally both SitAtTheTable
            freddy and sally both ensureThat(TheRoundPhase, withinTimeout) { Is(Bidding) }

            freddy and sally both Bid(1)
            freddy and sally both ensure(withinTimeout) {
                that(TheRoundPhase) { Is(TrickTaking) }
                that(TheTrickNumber) { Is(TrickNumber.of(1)) }
                that(TheCurrentPlayer) { Is(freddy.playerId) }
            }
        }

        @Test
        fun `when all players have played their card, the next trick or round automatically begins`() {
            data class RoundState(
                val round: Int = RoundNumber.None.value,
                val phase: RoundPhase?,
                val trick: Int = TrickNumber.None.value,
            )

            val TheRoundState = Question("about the round state") { actor ->
                RoundState(
                    round = actor.asksAbout(TheRoundNumber).value,
                    phase = actor.asksAbout(TheRoundPhase),
                    trick = actor.asksAbout(TheTrickNumber).value,
                )
            }

            freddy and sally both SitAtTheTable
            freddy and sally both ensure(withinTimeout) {
                that(TheRoundState) { Is(RoundState(round = 1, phase = Bidding)) }
            }

            freddy and sally both Bid(1)
            freddy and sally both ensure(withinTimeout) {
                that(TheRoundState) { Is(RoundState(round = 1, phase = TrickTaking, trick = 1)) }
            }

            freddy and sally both Play.theirFirstPlayableCard
            freddy and sally both ensure(withinTimeout) {
                that(TheRoundState) { Is(RoundState(round = 2, phase = Bidding)) }
            }

            freddy and sally both Bid(1)
            freddy and sally both ensure(withinTimeout) {
                that(TheRoundState) { Is(RoundState(round = 2, phase = TrickTaking, trick = 1)) }
            }

            freddy and sally both Play.theirFirstPlayableCard
            freddy and sally both ensure(withinTimeout) {
                that(TheRoundState) { Is(RoundState(round = 2, phase = TrickTaking, trick = 2)) }
            }

            freddy and sally both Play.theirFirstPlayableCard
            freddy and sally both ensure(withinTimeout) {
                that(TheRoundState) { Is(RoundState(round = 3, phase = Bidding)) }
            }
        }
    }
}

private infix fun Actor.won(count: Int) = this.playerId to count

internal object TestHelpers : Ensurer by ensurer() {
    fun playUpToStartOf(round: RoundNumber, trick: TrickNumber, theGameMaster: Actor, thePlayers: List<Actor>) {
        require(round.value != 1) { "playing to round 1 not implemented" }
        require(trick.value == 1) { "trick greater than 1 not implemented" }

        playUpTo(round - 1, theGameMaster, thePlayers)
        theGameMaster(SaysTheRoundCanStart)
        thePlayers each ensure {
            that(TheRoundNumber) { Is(round) }
            that(TheRoundPhase) { Is(Bidding) }
        }

        thePlayers each Bid(1)
        playUpToAndIncluding(trick - 1, theGameMaster, thePlayers)
        theGameMaster(SaysTheNextTrickCanStart)

        thePlayers each ensure {
            that(TheRoundNumber) { Is(round) }
            that(TheRoundPhase) { Is(TrickTaking) }
            that(TheTrickNumber) { Is(trick) }
        }
    }

    fun skipToTrickTaking(theGameMaster: Actor, thePlayers: List<Actor>) {
        val gameState = thePlayers.first().asksAbout(TheGameState)
        require(gameState == InProgress) { "cannot skip to trick taking when the game is $gameState" }

        when (thePlayers.first().asksAbout(TheRoundPhase)) {
            Bidding -> {
                thePlayers each Bid(1)
                theGameMaster(SaysTheTrickCanStart)
            }

            BiddingCompleted -> theGameMaster(SaysTheTrickCanStart)
            TrickTaking -> error("already in the trick taking phase")
            TrickCompleted -> {
                val trickNumber = thePlayers.first().asksAbout(TheTrickNumber)
                val roundNumber = thePlayers.first().asksAbout(TheRoundNumber)
                if (trickNumber.value == roundNumber.value) theGameMaster(SaysTheRoundCanStart)
                thePlayers each ensureThat(TheRoundPhase) { Is(Bidding) }
                thePlayers each Bid(1)
                theGameMaster(SaysTheTrickCanStart)
            }

            null -> error("the game probably hasn't started yet")
        }

        thePlayers each ensure {
            that(TheRoundPhase) { Is(TrickTaking) }
            that(TheTrickNumber) { Is(TrickNumber.of(1)) }
        }
    }

    fun playUpTo(endOfRound: RoundNumber, theGameMaster: Actor, thePlayers: List<Actor>) {
        thePlayers each SitAtTheTable
        theGameMaster(SaysTheGameCanStart)

        (1..endOfRound.value).forEach { roundNumber ->
            if (roundNumber != 1) {
                theGameMaster(SaysTheRoundCanStart)
            }

            thePlayers each ensure {
                that(TheRoundNumber) { Is(RoundNumber.of(roundNumber)) }
                that(TheRoundPhase) { Is(Bidding) }
            }
            thePlayers each Bid(1)
            playUpToAndIncluding(
                trick = TrickNumber.of(roundNumber),
                theGameMaster = theGameMaster,
                thePlayers = thePlayers
            )
        }
    }

    fun playUpToAndIncluding(trick: TrickNumber, theGameMaster: Actor, thePlayers: List<Actor>) {
        require(
            thePlayers.first().asksAbout(TheRoundPhase) == BiddingCompleted
        ) { "must start from bidding completion" }
        if (trick < 1) return

        (1..trick.value).forEach { trickNumber ->
            theGameMaster(SaysTheTrickCanStart)
            thePlayers each ensure {
                that(TheRoundPhase) { Is(TrickTaking) }
                that(TheTrickNumber) { Is(TrickNumber.of(trickNumber)) }
            }

            thePlayers each Play.theirFirstPlayableCard
            thePlayers each ensureThat(TheRoundPhase) { Is(TrickCompleted) }
        }
    }
}

internal fun Card.playedBy(actor: Actor): PlayedCard = this.playedBy(actor.playerId)

internal infix fun Actor.and(other: Actor) = this to other
internal infix fun Pair<Actor, Actor>.and(other: Actor) = listOf(first, second, other)

infix fun Actor.whoBid(bid: Int) = Pair(this.playerId, Placed(Bid.of(bid)))
fun Actor.whoseBidIsHidden() = Pair(this.playerId, Hidden)
fun Actor.whoHasNotBid() = Pair(this.playerId, None)
