function connectToWs(wsAddress) {
    const socket = new WebSocket(wsAddress);
    configureWs()
    configureElementCallbacks()

    function configureWs() {
        const handlers = newGameEventHandlers()

        socket.addEventListener("close", (event) => {
            console.error("disconnected from ws")
        })

        socket.addEventListener("message", (event) => {
            console.log("Message from server ", event.data);

            try {
                const data = JSON.parse(event.data)
                switch (data.type) {
                    // TODO: all of this stuff could do with some furniture arranging
                    case "GameEvent$PlayerJoined": return handlers.playerJoined(data);
                    case "GameEvent$GameStarted": return handlers.gameStarted();
                    case "GameEvent$RoundStarted": return handlers.roundStarted(data);
                    case "GameEvent$BetPlaced": return handlers.betPlaced(data);
                    case "GameEvent$BettingCompleted": return handlers.bettingCompleted(data);
                    case "GameEvent$CardPlayed": return handlers.cardPlayed(data);
                    default: {
                        socket.send(JSON.stringify({
                            type: "ClientMessage$UnhandledMessageFromServer",
                            offender: data.type,
                        }))
                        console.error(`Unknown message from server: ${data.type}`)
                    }
                }
            } catch (e) {
                socket.send(JSON.stringify({
                    stackTrace: e.stack,
                    type: "ClientMessage$Error",
                }))
                throw e
            }
        });

        function newGameEventHandlers() {
            return {
                playerJoined: function(gameEvent) {
                    const players = document.getElementById("players")
                    const li = document.createElement("li")
                    li.innerText = gameEvent.playerId
                    players.appendChild(li)

                    if (!gameEvent.waitingForMorePlayers) {
                        const gameStateEl = document.getElementById("gameState")
                        gameStateEl.innerText = ""
                    }
                },
                gameStarted: function() {
                    const gameStateEl = document.getElementById("gameState")
                    gameStateEl.innerText = "The game has started :D"

                    const gamePhaseEl = document.getElementById("gamePhase")
                    gamePhaseEl.innerText = "Place your bid!"
                },
                roundStarted: function(gameEvent) {
                    const handEl = document.getElementById("hand")
                    gameEvent.cardsDealt.forEach(card => {
                        const li = document.createElement("li")
                        li.innerText = card.id

                        const button = document.createElement("button")
                        button.innerText = "Play"
                        button.onclick = function playCard() {
                            li.remove()
                            socket.send(JSON.stringify({
                                type: "ClientMessage$CardPlayed",
                                cardId: card.id,
                            }))
                        }
                        li.appendChild(button)
                        handEl.appendChild(li)
                    })
                },
                betPlaced: function(gameEvent) {
                    const playersWhoHaveBet = document.getElementById("playersWhoHaveBet")
                    const li = document.createElement("li")
                    li.innerText = gameEvent.playerId
                    playersWhoHaveBet.appendChild(li)

                    if (gameEvent.isBettingComplete) {
                        const gamePhaseEl = document.getElementById("gamePhase")
                        gamePhaseEl.innerText = "It's trick taking time!"
                    }
                },
                bettingCompleted: function(gameEvent) {
                    const bets = gameEvent.bets
                    const betsEl = document.getElementById("bets")
                    betsEl.innerHTML = ""
                    Object.entries(bets).forEach(([playerId, bet]) => {
                        const li = document.createElement("li")
                        li.innerText = `${playerId}:${bet}`
                        betsEl.appendChild(li)
                    })
                },
                cardPlayed: function(gameEvent) {
                    const trick = document.getElementById("trick")
                    const li = document.createElement("li")
                    li.innerText = `${gameEvent.playerId}:${gameEvent.cardId}`
                    trick.appendChild(li)
                },
            }
        }
    }

    function configureElementCallbacks() {
        window.onBetSubmit = function(){
            const bet = document.getElementsByName("bet")[0].value
            socket.send(JSON.stringify({
                type: "ClientMessage$BetPlaced",
                bet: bet,
            }))
        }
    }
}
