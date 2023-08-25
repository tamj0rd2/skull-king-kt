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
                const gameEvent = JSON.parse(event.data)
                switch (gameEvent.type) {
                    // TODO: all of this stuff could do with some furniture arranging
                    case "PlayerJoined": return handlers.playerJoined(gameEvent);
                    case "GameStarted": return handlers.gameStarted();
                    case "RoundStarted": return handlers.roundStarted(gameEvent);
                    case "BetPlaced": return handlers.betPlaced(gameEvent);
                    case "BettingCompleted": return handlers.bettingCompleted(gameEvent);
                    case "CardPlayed": return handlers.cardPlayed(gameEvent);
                    default: {
                        socket.send(JSON.stringify({
                            type: "ClientMessage$UnhandledGameEvent",
                            offender: gameEvent.type,
                        }))
                        console.error(`Unknown game event ${gameEvent.type}`)
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
