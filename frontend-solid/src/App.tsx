import './App.css'
import {Component, createSignal, Show} from "solid-js";
import {PlayerId} from "../generated_types.ts";

const [playerId, setPlayerId] = createSignal<PlayerId>()
const [hasJoined, setHasJoined] = createSignal(false)

const App: Component = () => {
  return (
    <>
      <Show when={hasJoined()} fallback={<h1>Game Page</h1>}>
        <h1>Game Page - {playerId()}</h1>
      </Show>
      <form id="joinGame" onSubmit={(e) => {
        e.preventDefault()
        e.stopPropagation()
        setHasJoined(true)
      }}>
        <label>Player ID:
          <input type="text" name="playerId" onInput={(e) => setPlayerId(e.target.value)}></input>
        </label>
        <input type="submit">Join game</input>
      </form>
    </>
  )
}

export default App
