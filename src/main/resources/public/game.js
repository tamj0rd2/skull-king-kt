function connectToWs(wsAddress) {
    const socket = new WebSocket(wsAddress);

    configureWs()
    configureElementCallbacks()

    function configureWs() {
        const handlers = newGameEventHandlers()
        const body = document.querySelector("body")
        const biddingForm = document.createElement("sk-biddingform")
        const bidsEl = document.createElement("sk-bids")

        socket.addEventListener("close", (event) => {
            console.error("disconnected from ws")
        })

        socket.addEventListener("message", (event) => {
            console.log("Message from server ", event.data);

            try {
                const data = JSON.parse(event.data)
                switch (data.type) {
                    case "GameEvent$PlayerJoined": return handlers.playerJoined(data);
                    case "GameEvent$GameStarted": return handlers.gameStarted(data);
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

                    // TODO: I don't think I need this anymore? Or I should have a message saying waiting to start
                    if (!gameEvent.waitingForMorePlayers) {
                        const gameStateEl = document.getElementById("gameState")
                        gameStateEl.innerText = ""
                    }
                },
                gameStarted(gameEvent) {
                    const gameStateEl = document.getElementById("gameState")
                    gameStateEl.innerText = "The game has started :D"
                    body.appendChild(biddingForm)

                    body.appendChild(bidsEl)
                    bidsEl.gameStarted(gameEvent.players)
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
                betPlaced({ playerId }) {
                    bidsEl.handleBidPlaced(playerId)
                },
                bettingCompleted({ bets }) {
                    updateGamePhase("TrickTaking")
                    bidsEl.handleBiddingCompleted(bets)
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

    class BiddingForm extends HTMLElement {
        constructor() {
            super()
        }

        connectedCallback() {
            this.innerHTML = `
                <label>Bid <input type="number" name="bet" min="0" max="10"></label>
                <button id="placeBet" type="button" onclick="onBetSubmit()">Place Bet</button>
                <p id="biddingError"></p>
            `

            const bidInput = this.querySelector(`input[name="bet"]`)
            this.querySelector("#placeBet").onclick = function(){
                socket.send(JSON.stringify({
                    type: "ClientMessage$BetPlaced",
                    bet: bidInput.value,
                }))
            }
        }
    }

    class Bids extends HTMLElement {
        constructor() {
            super();
        }

        connectedCallback() {
            this.innerHTML = `
                <section id="betsArea">
                    <h3>Bets</h3>
                    <ul id="bets"></ul>
                </section>
            `
        }

        gameStarted(playerIds) {
            const betsEl = this.querySelector("#bets")
            playerIds.forEach(playerId => {
                const li = document.createElement("li")
                li.textContent = playerId
                li.setAttribute("data-playerBet", playerId)
                li.appendChild(document.createElement("span"))
                betsEl.appendChild(li)
            })
        }

        handleBidPlaced(playerId) {
            this.querySelector(`[data-playerBet="${playerId}"] span`).innerText = ":" + "has bet"
        }

        handleBiddingCompleted(bets) {
            this.querySelectorAll(`[data-playerBet]`).forEach(el => {
                const playerId = el.getAttribute("data-playerBet")
                const bet = bets[playerId]
                el.querySelector("span").innerText = ":" + bet
            })
        }
    }

    customElements.define("sk-biddingform", BiddingForm)
    customElements.define("sk-bids", Bids)
}
