package main

import (
	"bytes"
	"errors"
	"fmt"
	"golang.org/x/net/html"
	"io"
	"io/ioutil"
	"log"
	"net/http"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
)

const localFilePath = "/mnt/usb"
const urlPrefix = "http://192.168.1.254:80"
const webserverPath = "YICARCAM/MOVIE_S"

func downloadFromUrl(url string, targetDirectory string, failIfTargetExists bool, cleanupFailedDownload bool) (int64, error) {

	tokens := strings.Split(url, "/")
	fileName := tokens[len(tokens)-1]
	filePath := filepath.Join(targetDirectory, fileName)

	//log.Println("Downloading", url, "to", filePath)

	if _, err := os.Stat(filePath); (err == nil) && failIfTargetExists {
		return 0, errors.New(fmt.Sprintf("Error: file exists (%s), cannot proceed", filePath))
	}

	response, err := http.Get(url)
	if err != nil {
		return 0, errors.New(fmt.Sprintf("Error while accessing URL %s - %s", url, err))
	}
	defer response.Body.Close()

	output, err := os.Create(filePath)
	if err != nil {
		return 0, errors.New(fmt.Sprintf("Error while creating local file %s - %s", fileName, err))
	}
	defer output.Close()

	n, err := io.Copy(output, response.Body)
	if err != nil {
		if cleanupFailedDownload {
			os.Remove(filePath)
		}
		return 0, errors.New(fmt.Sprintf("Error while downloading from URL %s - %s", url, err))
	} else {
		if n == 128 {
			// TODO: check if file created has "<head><title>Page Not found</title></head>"
			os.Remove(filePath)
			return 0, errors.New(fmt.Sprintf("Error while downloading from URL %s - %s", url, err))
		}
	}

	return n, nil

}

func processResponse(file []byte) {

	var (
		fileList []string
	)

	doc, err := html.Parse(bytes.NewReader(file))

	if err != nil {
		log.Fatal(err)
	}

	hrefRegexp, _ := regexp.Compile("del=1")

	var f func(*html.Node)

	f = func(n *html.Node) {
		if /*n.Type == html.ElementNode &&*/ n.Data == "a" {
			// log.Print(n.Data)
			for _, attr := range n.Attr {
				if attr.Key == "href" {
					if hrefRegexp.MatchString(attr.Val) {
						//log.Println(attr.Val)
						elem := strings.Split(attr.Val, "/")
						if len(elem) >= 3 {
							elem2 := strings.Split(elem[3], "?")
							if len(elem2) >= 2 {
								cameraFile := elem2[0]
								// log.Println("HTML parser: processing " + cameraFile)
								fileList = append(fileList, cameraFile)
								// downloadFile(cameraFile)
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

	// enter the recursion
	f(doc)

	sort.Strings(fileList)

	for _, fileName := range fileList[0 : len(fileList)-1] {
		url := urlPrefix + "/" + webserverPath + "/" + fileName
		_, err := downloadFromUrl(url, localFilePath, true, true)
		if (err != nil) && (strings.HasPrefix(err.Error(), "Error: file exists")) {
			log.Println(err)
		}
	}

}

func getResponseFromHttp() []byte {

	var (
		err      error
		url      string
		req      *http.Request
		resp     *http.Response
		client   *http.Client
		respBody []byte
	)

	url = urlPrefix + "/" + webserverPath

	req, err = http.NewRequest("GET", url, nil)

	if err != nil {
		log.Fatal("NewRequest: ", err)
	}

	client = &http.Client{}

	resp, err = client.Do(req)
	defer resp.Body.Close()

	if err != nil {
		log.Fatal("Error sending HTTP request via client: ", err)
	}

	respBody, _ = ioutil.ReadAll(resp.Body)

	return respBody

}

func getResponseFromFile() []byte {

	var (
		err      error
		filePath string
		currdir  string
		respBody []byte
	)

	currdir, _ = os.Getwd()

	filePath = filepath.Join(currdir, "tests", "fixtures", "index.html")

	if _, err := os.Stat(filePath); os.IsNotExist(err) {
		log.Fatalf("Error: json response file does not exist: %s", filePath)
	}

	respBody, err = ioutil.ReadFile(filePath)

	if err != nil {
		log.Fatal("File error: ", err)
	}

	return respBody

}

func getFileList() {

	var (
		FileListResponseRaw []byte
	)

	// TODO: remove me
	const readFromFile = false

	// callSecureHttpService(&appConfig)
	if readFromFile {
		FileListResponseRaw = getResponseFromFile()
	} else {
		FileListResponseRaw = getResponseFromHttp()
	}

	processResponse(FileListResponseRaw)

}

func main() {

	getFileList()

}
