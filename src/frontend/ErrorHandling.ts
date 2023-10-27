import {ErrorCode} from "./Socket";

export function showErrorMessage(message: string, code?: ErrorCode) {
    console.log("attempting to show", message, code)
    const el = document.querySelector("#errorMessage")!!
    el.setAttribute("errorCode", (code ?? "unknown").toString())
    el.textContent = message
    el.classList.remove("u-hidden")
}
