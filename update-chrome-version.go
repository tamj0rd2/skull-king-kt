package main

import (
	"fmt"
	"log"
	"os"
	"regexp"
	"strings"
)
import "net/http"

func main() {
	latestChromeVersion := "v" + strings.TrimSpace(os.Args[1])

	client := &http.Client{
		CheckRedirect: func(req *http.Request, via []*http.Request) error {
			return http.ErrUseLastResponse
		},
	}

	res, err := client.Get("https://github.com/SeleniumHQ/selenium/releases/latest")
	if err != nil {
		log.Fatal(err)
	}
	defer res.Body.Close()

	location := strings.TrimSpace(res.Header.Get("Location"))
	i := strings.LastIndex(location, "-")
	latestSeleniumVersion := location[i+1:]

	b, err := os.ReadFile("build.gradle.kts")
	if err != nil {
		log.Fatal(err)
	}
	fileContent := string(b)

	currentSeleniumVersionString := regexp.MustCompile(`(seleniumVersion = ".*")`).FindStringSubmatch(fileContent)[1]
	fileContent = strings.Replace(fileContent, currentSeleniumVersionString, fmt.Sprintf(`seleniumVersion = %q`, latestSeleniumVersion), 1)

	chromeVersionString := regexp.MustCompile(`(chromeVersion = ".*")`).FindStringSubmatch(fileContent)[1]
	fileContent = strings.Replace(fileContent, chromeVersionString, fmt.Sprintf(`chromeVersion = %q`, latestChromeVersion), 1)

	if err := os.WriteFile("build.gradle.kts", []byte(fileContent), 0644); err != nil {
		log.Fatal(err)
	}

	fmt.Printf("Updated build.gradle.kts with latest selenium (%s) and chrome (%s) version\n", latestSeleniumVersion, latestChromeVersion)
}
