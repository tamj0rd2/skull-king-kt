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
                    case "GameEvent$PlayerJoined": return handlers.playerJoined(data);
                    case "GameEvent$GameStarted": return handlers.gameStarted();
                    case "GameEvent$RoundStarted": return handlers.roundStarted(data);
                    case "GameEvent$BetPlaced": return handlers.betPlaced(data);
                    case "GameEvent$BettingCompleted": return handlers.bettingCompleted(data);
                    case "GameEvent$CardPlayed": return handlers.cardPlayed(data);
                    case "GameEvent$TrickCompleted": return handlers.trickCompleted(data);
                    case "GameEvent$TrickStarted": return handlers.trickStarted(data);
                    case "GameEvent$GameCompleted": return handlers.gameCompleted(data);
                    case "ErrorToClient": return handlers.errorFromServer(data);
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

            function updateGamePhase(gamePhase) {
                const gamePhaseEl = document.getElementById("gamePhase")
                const gamePhaseMapping = {
                    "Bidding": "Place your bid!",
                    "TrickTaking": "It's trick taking time!",
                    "TrickComplete": "Trick completed :)"
                }

                const text = gamePhaseMapping[gamePhase]
                if (!text) throw new Error("Unknown game phase: " + gamePhase)
                gamePhaseEl.innerText = gamePhaseMapping[gamePhase]
            }

            return {
                playerJoined(gameEvent) {
                    const players = document.getElementById("players")
                    const li = document.createElement("li")
                    li.innerText = gameEvent.playerId
                    players.appendChild(li)

                    const betsEl = document.getElementById("bets")
                    const li2 = document.createElement("li")
                    li2.innerText = gameEvent.playerId
                    li2.setAttribute("data-playerBet", gameEvent.playerId)
                    li2.appendChild(document.createElement("span"))
                    betsEl.appendChild(li2)

                    if (!gameEvent.waitingForMorePlayers) {
                        const gameStateEl = document.getElementById("gameState")
                        gameStateEl.innerText = ""
                    }
                },
                gameStarted() {
                    const gameStateEl = document.getElementById("gameState")
                    gameStateEl.innerText = "The game has started :D"
                },
                roundStarted(gameEvent) {
                    const roundNumberEl = document.getElementById("roundNumber")
                    roundNumberEl.innerText = gameEvent.roundNumber
                    updateGamePhase("Bidding")

                    const trickNumberEl = document.getElementById("trickNumber")
                    trickNumberEl.innerText = ""

                    const betEl = document.getElementsByName("bet")[0]
                    betEl.value = ""

                    const trick = document.getElementById("trick")
                    trick.textContent = ""

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
                betPlaced(gameEvent) {
                    document.querySelector(`[data-playerBet="${gameEvent.playerId}"] span`).innerText = ":" + "has bet"
                },
                bettingCompleted(gameEvent) {
                    updateGamePhase("TrickTaking")

                    document.querySelectorAll(`[data-playerBet]`).forEach(el => {
                        const playerId = el.getAttribute("data-playerBet")
                        const bet = gameEvent.bets[playerId]
                        el.getElementsByTagName("span")[0].innerText = ":" + bet
                    })
                },
                cardPlayed(gameEvent) {
                    const trick = document.getElementById("trick")
                    const li = document.createElement("li")
                    li.innerText = `${gameEvent.playerId}:${gameEvent.cardId}`
                    trick.appendChild(li)
                },
                trickStarted(gameEvent) {
                    const trickNumberEl = document.getElementById("trickNumber")
                    trickNumberEl.innerText = gameEvent.trickNumber

                    const trick = document.getElementById("trick")
                    trick.textContent = ""
                },
                trickCompleted(gameEvent) {
                    updateGamePhase("TrickComplete")
                },
                gameCompleted(gameEvent) {
                    const gameStateEl = document.getElementById("gameState")
                    gameStateEl.innerText = "The game is over!"
                },
                errorFromServer({errorCode}) {
                    switch (errorCode) {
                        case "NotStarted": {
                            const biddingError = document.getElementById("biddingError")
                            biddingError.innerText = "Cannot perform this action because the game has not started yet"
                            biddingError.setAttribute("data-errorCode", errorCode)
                            return
                        }
                        default: throw new Error("Unknown error code: " + errorCode)
                    }
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
