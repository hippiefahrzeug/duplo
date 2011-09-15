import groovy.swing.SwingBuilder  
import groovy.beans.Bindable  
import static javax.swing.JFrame.EXIT_ON_CLOSE  
import java.awt.*
import javax.swing.ImageIcon
import java.awt.image.BufferedImage
import java.security.MessageDigest
import javax.swing.*

env = System.getenv()
sizemap = [:]
possibledupes = []

def wait = true
stop = false

minSize = 100000
images = true

def intro = new SwingBuilder()

intro.edt {
    lookAndFeel 'system'  // Simple change in look and feel.
    frame(title: 'dupes', size: [300, 250],
            show: true, locationRelativeTo: null,
            defaultCloseOperation: EXIT_ON_CLOSE) {
               panel(constraints: BorderLayout.WEST) {
               tableLayout {
                    tr {
                        td {
                          label text:"file selection:"
                        }
                    }
                    tr {
                        td {
                          selfolder = textField(text:"${env["HOME"]}")
                          selfolder.preferredSize = new Dimension(250, 30)
                        }
                    }
                    tr {
                        td {
                          minSize = textField(text:minSize)
                          minSize.preferredSize = new Dimension(250, 30)
                       }
                    }
                    tr {
                        td {
                               imagesOnly = radioButton(text:"images only", selected: images)
                        }
                    }
                    tr {
                        td {
                              startBut = button(text: 'start', actionPerformed: {
                                  if (startBut.text == "start") {
                                      wait = false
                                      startBut.text = "stop"
                                  }
                                  else {
                                      stop = true
                                  }
                              })
                        }
                    }
                    tr {
                        td {
                          label text:"---------------------------------"
                        }
                    }
                    tr {
                        td {
                            vbox {
                                info1 = label(text:"-")
                                info2 = label(text:"-")
                                info3 = label(text:"-")
                            }
                        }
                    }
                 }
             }
         }
}

// this is really quite bad. just couldn't figure out a better way for now.
while(wait) {
    sleep 500
}

def (origs, dupes, sizes) = finddupes()

def deleteMap = [:]
def swingBuilder = new SwingBuilder()
def imageLabelsToLoad = []
def imagesToLoad = []
swingBuilder.edt {
    lookAndFeel 'system'  // Simple change in look and feel.
    frame(title: 'dupes', size: [800, 600],
            show: true, locationRelativeTo: null,
            defaultCloseOperation: EXIT_ON_CLOSE) {
        borderLayout(vgap: 5)

        scrollPane() {
           panel(constraints: BorderLayout.CENTER,
                   border: compoundBorder([emptyBorder(10)])) {
               tableLayout {
                   tr {
                        td {
                            vbox {
                                 button text: "delete ALL", actionPerformed: {
                                      deleteFiles(deleteMap)
                                 }
                            }
                        }
                   }
                 sizes.eachWithIndex { elem, idx -> 
                   if (idx > 1000) { return; }
                   def key = elem.key
                   def dups = dupes[key]
                   def img = origs[key]
                   def curDeleteMap = [:]
                   tr {
                        td {
                          if (isImage(img)) {
                           if (idx < 0) { // disabld for now
                            label icon:new ImageIcon(getScaledImage("$img", 64, 64)), mouseClicked: {
                                def p = ["display", "$img"].execute()
                                p.waitFor()
                            }
                           }
                           else {
                            def l = label(icon:new ImageIcon(), mouseClicked: {
                                def p = ["display", "$img"].execute()
                                p.waitFor()
                            })
                            imageLabelsToLoad.add(l)
                            imagesToLoad.add(img)
                           }
                          }
                          else {
                              label text:"no preview"
                          }
                        }
                        td {
                          panel(constraints: BorderLayout.SOUTH) {
                            vbox {
                                label("" + img.length())
                                   button text: "delete", actionPerformed: {
                                      deleteFiles(curDeleteMap)
                                 }
                            }
                          }
                        }
                        td {
                          panel(constraints: BorderLayout.SOUTH) {
                             tableLayout {
                                   tr {
                                      td {
                                          curDeleteMap[img] = false
                                          deleteMap[img] = false
                                          radioButton(selected: false, actionPerformed: {
                                           curDeleteMap[img] = it.source.selected
                                           deleteMap[img] = it.source.selected
                                          })
                                      }
                                      td {
                                          label(text:"${origs[key]}")
                                      }
                                   }
                                dups.each {
                                   def f = it
                                   curDeleteMap[f] = true
                                   tr {
                                      td {
                                         curDeleteMap[f] = true
                                         deleteMap[f] = true
                                         radioButton(selected: true, actionPerformed: {
                                           curDeleteMap[f] = it.source.selected
                                           deleteMap[f] = it.source.selected
                                          })
                                      }
                                      td {
                                          label(text:"$f")
                                      }
                                   }
                                }
                             }
                          }
                        }
                   }
                 }
               }
           }
        }
    }
}

imageLabelsToLoad.eachWithIndex {ele, idx ->
    println "loading: ${imagesToLoad[idx]}"
    def img = getScaledImage("${imagesToLoad[idx]}", 64, 64)
    ele.icon.image = img
    ele.text = "."
}

def deleteFiles(def map) {
    map.each {
        if (it.value) {
            println "delete $it.key"
            it.key.delete()
        }
    }
}

Image getScaledImage(String path, int w, int h){
        def i = new ImageIcon(path)
        BufferedImage resizedImg = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = resizedImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.drawImage(i.getImage(), 0, 0, w, h, null);
        g2.dispose();
        return resizedImg;
}

def finddupes() {
    def result = [:]
    def folder = selfolder.text
    def basedir = new File(folder)
    def all = []
    info1.text = "searching for files..."
    allFiles(basedir, 0, all);
    info1.text = "files found: ${all.size()} (${possibledupes.size()} need check)"

    def sizeCheck = [:]
    def map = [:]
    def sizes = [:]
    int n = 0;
    int c = 0;
    int wasted = 0;
    possibledupes.each {
        if (c++ % 101 == 0) {
            info2.text = "processing: " + "${possibledupes.size()-c} to go..."
            def megs = (int)(wasted/(1024*1024))
            println "${possibledupes.size()-c} to go... ($it)"
        }

        if (stop) {
            return
        }

        md5 = md5sum(it)
        if (map.containsKey(md5)) {
            sizes[md5] = it.length()
            // println "$it exists: ${map[md5]}"
            wasted += it.length()
            if (n++ % 31) {
                def megs = (int)(wasted/(1024*1024))
                println "$n duplicates found (wasting $megs mb)"
                info3.text = "$n duplicates found (wasting $megs mb)"
            }
            if (result.containsKey(md5)) {
               def l = result[md5]
               l.add(it);
            }
            else {
               def l = []
               l.add(it)
               result[md5] = l
            }
        }
        else {
            map[md5] = it
        }
        // print "openssl md5 $it".execute().text
    }
    info2.text = "processing: " + "${possibledupes.size()-c} to go..."

    sizes = sizes.sort {a, b -> b.value <=> a.value}
    sizes.each{println it}

    return [map, result, sizes];
}

def md5sum(File f) {
/* this is very slow :(
    MessageDigest digest = MessageDigest.getInstance("MD5")
    digest.update(f.text.bytes)
    BigInteger big = new BigInteger(1,digest.digest())
    String md5 = big.toString(16).padLeft(32,"0")
*/
    String md5 = ["openssl", "md5", "$f"].execute().text.replaceFirst(/.*= /, "").replace("\n", "")
    return md5;
}

def allFiles(File cur, int depth, def result) {
    if (!cur.canRead()) {
        return
    }
    cur.listFiles().sort().each {
        if (it.name.startsWith(".")) {
            return;
        }
        if (it.isFile() && isSelected(it)) {
            if (it.length() >= minSize.text.toInteger()) { // only look at bigger pics
                if (sizemap.containsKey(it.length())) {
                    if (sizemap[it.length()]) {
                        possibledupes.add(sizemap[it.length()])
                    }
                    possibledupes.add(it)
                    sizemap[it.length()] = null
                }
                else {
                    sizemap[it.length()] = it
                }
                result.add(it);
                if ((1+result.size()) % 33 == 0) {
                    info1.text = "files found: ${result.size()} (${possibledupes.size()} need check)"
                    println "${result.size()} files (... $cur)"
                }
            }
        }
        if (it.isDirectory()) {
            allFiles(it, depth+1, result)
        }
    }
    if (depth == 0 && cur.isDirectory()) {
        info1.text = "files found: " + result.size()
        println "$cur: ${result.size()} files"
    }
}

boolean isSelected(def it) {
    if (imagesOnly.selected) {
        return isImage(it);
    }
    return true;
}

boolean isImage(def it) {
    def n = it.name.toLowerCase()
    return n.endsWith(".jpg") || n.endsWith(".gif") || n.endsWith(".png") || n.endsWith(".tif") || n.endsWith(".tiff")
}
