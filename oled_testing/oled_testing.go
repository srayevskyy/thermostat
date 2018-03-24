package main

import (
	"github.com/goiot/devices/monochromeoled"
	"golang.org/x/exp/io/i2c"
	"golang.org/x/image/font"
	"golang.org/x/image/font/basicfont"
	"golang.org/x/image/math/fixed"
	"image"
	"image/color"
)

func addLabel(img *image.RGBA, x, y int, label string) {
	col := color.RGBA{200, 100, 0, 255}
	point := fixed.Point26_6{fixed.Int26_6(x * 64), fixed.Int26_6(y * 64)}

	d := &font.Drawer{
		Dst:  img,
		Src:  image.NewUniform(col),
		Face: basicfont.Face7x13, /*inconsolata.Regular8x16*/
		Dot:  point,
	}
	d.DrawString(label)
}

func oledTest() {
	img := image.NewRGBA(image.Rect(0, 0, 128, 64))
	addLabel(img, 0, 20, "TEST")

	d, err := monochromeoled.Open(&i2c.Devfs{Dev: "/dev/i2c-1"})
	if err != nil {
		panic(err)
	}

	if err := d.SetImage(0, 0, img); err != nil {
		panic(err)
	}

	if err := d.Draw(); err != nil {
		panic(err)
	}
	d.Close()
}

func main() {
	oledTest()
}
