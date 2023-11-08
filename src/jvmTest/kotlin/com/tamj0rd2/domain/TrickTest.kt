package com.tamj0rd2.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory

class TrickTest {
    private val adam = PlayerId("adam")
    private val becky = PlayerId("becky")
    private val cecil = PlayerId("cecil")
    private val danny = PlayerId("danny")

    @TestFactory
    fun `the winner is determined correctly`(): List<DynamicTest> {
        data class TestCase(
            val playedCards: List<PlayedCard>,
            val expectedWinner: PlayerId,
            val only: Boolean = false
        ) {
            override fun toString(): String {
                return playedCards.joinToString(", ") { "${it.playerId}:${it.card.name}" }
            }
        }

        val testCases = listOf(
            TestCase(
                playedCards = listOf(
                    1.blue.playedBy(adam),
                    2.blue.playedBy(becky),
                ),
                expectedWinner = becky
            ),
            TestCase(
                playedCards = listOf(
                    2.blue.playedBy(adam),
                    1.blue.playedBy(becky),
                ),
                expectedWinner = adam
            ),
            TestCase(
                playedCards = listOf(
                    1.blue.playedBy(adam),
                    2.red.playedBy(becky)
                ),
                expectedWinner = adam
            ),
            TestCase(
                playedCards = listOf(
                    1.blue.playedBy(adam),
                    2.black.playedBy(becky)
                ),
                expectedWinner = becky
            ),
            // black trumps all other suits
            TestCase(
                playedCards = listOf(
                    1.red.playedBy(adam),
                    2.black.playedBy(becky),
                    13.red.playedBy(cecil)
                ),
                expectedWinner = becky
            ),
            TestCase(
                playedCards = listOf(
                    1.black.playedBy(adam),
                    2.black.playedBy(becky)
                ),
                expectedWinner = becky
            ),
            TestCase(
                playedCards = listOf(
                    3.black.playedBy(adam),
                    2.black.playedBy(becky)
                ),
                expectedWinner = adam,
            ),
            TestCase(
                playedCards = listOf(
                    Card.SpecialCard.escape.playedBy(adam),
                    2.black.playedBy(becky)
                ),
                expectedWinner = becky
            ),
            *SpecialSuit.values().map { specialSuit ->
                // a later played special card can never win against itself
                TestCase(
                    playedCards = listOf(
                        Card.SpecialCard(specialSuit).playedBy(adam),
                        Card.SpecialCard(specialSuit).playedBy(becky)
                    ),
                    expectedWinner = adam,
                )
            }.toTypedArray(),
            *listOf(Card.SpecialCard.mermaid, Card.SpecialCard.pirate, Card.SpecialCard.skullKing).flatMap {
                listOf(
                    TestCase(
                        playedCards = listOf(
                            13.black.playedBy(adam),
                            it.playedBy(becky)
                        ),
                        expectedWinner = becky,
                    ),
                    TestCase(
                        playedCards = listOf(
                            it.playedBy(adam),
                            13.black.playedBy(becky),
                        ),
                        expectedWinner = adam,
                    ),
                    TestCase(
                        playedCards = listOf(
                            it.playedBy(adam),
                            Card.SpecialCard.escape.playedBy(becky),
                        ),
                        expectedWinner = adam,
                    ),
                    TestCase(
                        playedCards = listOf(
                            Card.SpecialCard.escape.playedBy(adam),
                            it.playedBy(becky),
                        ),
                        expectedWinner = becky,
                    ),
                )
            }.toTypedArray(),
            // rock paper scissors
            TestCase(
                playedCards = listOf(
                    Card.SpecialCard.skullKing.playedBy(adam),
                    Card.SpecialCard.mermaid.playedBy(becky),
                ),
                expectedWinner = becky,
            ),
            TestCase(
                playedCards = listOf(
                    Card.SpecialCard.mermaid.playedBy(adam),
                    Card.SpecialCard.skullKing.playedBy(becky),
                ),
                expectedWinner = adam,
            ),
            TestCase(
                playedCards = listOf(
                    Card.SpecialCard.mermaid.playedBy(adam),
                    Card.SpecialCard.pirate.playedBy(becky),
                ),
                expectedWinner = becky,
            ),
            TestCase(
                playedCards = listOf(
                    Card.SpecialCard.pirate.playedBy(adam),
                    Card.SpecialCard.mermaid.playedBy(becky),
                ),
                expectedWinner = adam,
            ),
            // mermaid trumps pirate if skull king is played
            TestCase(
                playedCards = listOf(
                    Card.SpecialCard.mermaid.playedBy(adam),
                    Card.SpecialCard.pirate.playedBy(becky),
                    Card.SpecialCard.skullKing.playedBy(cecil),
                    Card.SpecialCard.mermaid.playedBy(danny)
                ),
                expectedWinner = adam
            ),
            TestCase(
                playedCards = listOf(
                    Card.SpecialCard.pirate.playedBy(adam),
                    Card.SpecialCard.skullKing.playedBy(becky),
                ),
                expectedWinner = becky,
            ),
            TestCase(
                playedCards = listOf(
                    Card.SpecialCard.skullKing.playedBy(adam),
                    Card.SpecialCard.pirate.playedBy(becky),
                ),
                expectedWinner = adam,
            ),
        )

        return testCases
            .let { tcs -> if (tcs.any { it.only }) tcs.filter { it.only } else tcs }
            .map { tc ->
                DynamicTest.dynamicTest(tc.toString()) {
                    val trick = Trick(tc.playedCards.size)
                    tc.playedCards.forEach { trick.add(it) }

                    trick.isComplete shouldBe true
                    trick.winner shouldBe tc.expectedWinner
                }
            }
    }
}
