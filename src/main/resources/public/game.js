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
                gameStarted: function() {
                    const gameStateEl = document.getElementById("gameState")
                    gameStateEl.innerText = "The game has started :D"

                    const gamePhaseEl = document.getElementById("gamePhase")
                    gamePhaseEl.innerText = "Place your bid!"
                },
                roundStarted: function(gameEvent) {
                    const roundNumberEl = document.getElementById("roundNumber")
                    roundNumberEl.innerText = gameEvent.roundNumber

                    const trickNumberEl = document.getElementById("trickNumber")
                    trickNumberEl.innerText = gameEvent.trickNumber

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
                betPlaced: function(gameEvent) {
                    document.querySelector(`[data-playerBet="${gameEvent.playerId}"] span`).innerText = ":" + "has bet"
                },
                bettingCompleted: function(gameEvent) {
                    const bets = gameEvent.bets
                    document.querySelectorAll(`[data-playerBet]`).forEach(el => {
                        const gamePhaseEl = document.getElementById("gamePhase")
                        gamePhaseEl.innerText = "It's trick taking time!"

                        const playerId = el.getAttribute("data-playerBet")
                        const bet = bets[playerId]
                        el.getElementsByTagName("span")[0].innerText = ":" + bet
                    })
                },
                cardPlayed: function(gameEvent) {
                    const trick = document.getElementById("trick")
                    const li = document.createElement("li")
                    li.innerText = `${gameEvent.playerId}:${gameEvent.cardId}`
                    trick.appendChild(li)
                },
                trickStarted: function(gameEvent) {
                    const trickNumberEl = document.getElementById("trickNumber")
                    trickNumberEl.innerText = gameEvent.trickNumber

                    const trick = document.getElementById("trick")
                    trick.textContent = ""
                },
                trickCompleted: function(gameEvent) {
                    const gamePhaseEl = document.getElementById("gamePhase")
                    gamePhaseEl.innerText = "Trick completed :)"
                },
                gameCompleted: function (gameEvent) {
                    const gameStateEl = document.getElementById("gameState")
                    gameStateEl.innerText = "The game is over!"
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
