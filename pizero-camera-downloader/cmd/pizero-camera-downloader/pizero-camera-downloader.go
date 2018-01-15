package main

import (
	"fmt"
	"golang.org/x/net/html"
	"io"
	"log"
	"net/http"
	"os"
	"path"
	"path/filepath"
	"regexp"
	"strings"
)

// app config types

const localFilePath = "/Users/serge/Downloads/files"
const urlPrefix = "http://192.168.1.254:80"
const webserverPath = "YICARCAM/MOVIE_S"

func downloadFromUrl(url string, targetDirectory string) {
	tokens := strings.Split(url, "/")
	fileName := tokens[len(tokens)-1]
	fmt.Println("Downloading", url, "to", filepath.Join(targetDirectory, fileName))

	// TODO: check file existence first with io.IsExist
	output, err := os.Create(filepath.Join(targetDirectory, fileName))
	if err != nil {
		fmt.Println("Error while creating", fileName, "-", err)
		return
	}
	defer output.Close()

	response, err := http.Get(url)
	if err != nil {
		fmt.Println("Error while downloading", url, "-", err)
		return
	}
	defer response.Body.Close()

	n, err := io.Copy(output, response.Body)
	if err != nil {
		fmt.Println("Error while downloading", url, "-", err)
		return
	}

	fmt.Println(n, "bytes downloaded.")
}

func downloadFile(cameraFile string) {
	if _, err := os.Stat(path.Join(localFilePath, cameraFile)); os.IsNotExist(err) {
		url := urlPrefix + "/" + webserverPath + "/" + cameraFile
		downloadFromUrl(url, localFilePath)
	} else {
		fmt.Println("File " + cameraFile + " already downloaded, nothing to do")
	}
}

func processResponse(file io.Reader) {
	doc, err := html.Parse(file)

	if err != nil {
		log.Fatal(err)
	}

	hrefRegexp, _ := regexp.Compile("del=1")

	var f func(*html.Node)

	f = func(n *html.Node) {
		if /*n.Type == html.ElementNode &&*/ n.Data == "a" {
			// fmt.Print(n.Data)
			for _, attr := range n.Attr {
				if attr.Key == "href" {
					if hrefRegexp.MatchString(attr.Val) {
						//fmt.Println(attr.Val)
						elem := strings.Split(attr.Val, "/")
						if len(elem) >= 3 {
							elem2 := strings.Split(elem[3], "?")
							if len(elem2) >= 2 {
								cameraFile := elem2[0]
								fmt.Println("HTML parser: processing " + cameraFile)
								downloadFile(cameraFile)
							}
						}
					}
				}
			}
		}
		for c := n.FirstChild; c != nil; c = c.NextSibling {
			f(c)
		}
	}
	f(doc)
}

func getResponseFromHttp() {

	var (
		err    error
		url    string
		req    *http.Request
		resp   *http.Response
		client *http.Client
	)

	url = urlPrefix + "/" + webserverPath

	req, err = http.NewRequest("GET", url, nil)

	if err != nil {
		log.Fatal("NewRequest: ", err)
		return
	}

	client = &http.Client{}

	resp, err = client.Do(req)
	if err != nil {
		log.Fatal("Error sending HTTP request via client: ", err)
	}

	defer resp.Body.Close()

	processResponse(resp.Body)

}

func getResponseFromFile() {
	var (
		err error
	)

	file, err := os.Open("../tests/fixtures/index.html")

	if err != nil {
		log.Fatal("File error: ", err)
	}

	defer file.Close()

	processResponse(file)

}

func getFileList() {

	// TODO: remove me
	const readFromFile = false

	// callSecureHttpService(&appConfig)
	if readFromFile {
		getResponseFromFile()
	} else {
		getResponseFromHttp()
	}

}

func main() {

	getFileList()

}
