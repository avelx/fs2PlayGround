package processor

import edu.stanford.nlp.pipeline.StanfordCoreNLP

import java.util.Properties
import scala.util.Try

object TextProcessor {

  def exec(text: String): Unit = {
    // set up pipeline properties
    val props = new Properties()
    // set the list of annotators to run
    props.setProperty("annotators", "tokenize,pos,lemma,ner,parse,depparse,coref,kbp,quote")
    //props.setProperty("annotators", "tokenize,pos,lemma,ner,depparse")
    props.setProperty("coref.algorithm", "neural")
    // build pipeline
    val pipeline = new StanfordCoreNLP(props)

    // create a document object
    val coreDoc = pipeline.processToCoreDocument(text)
    //pipeline.annotate(coreDoc)

    println("Annotations")
    println(coreDoc.annotation())

    println("Sentence")
    coreDoc.sentences().forEach(s => {
      println(s"S: ${s.text()}")
      println(s"L: ${s.lemmas()}")
      println(s"T: ${s.tokens()}")
      println(s"Tags: ${s.nerTags()}")
      s.entityMentions().forEach{ c =>
        val cEm = Try {
          c.canonicalEntityMention().get().toString
        }.toOption
        println(s"EM: $cEm")
      }
    })

    println("Quotes")
    coreDoc.quotes().forEach(q => {
//      println(s"SPK: ${q.speaker()}")
    })

    println("CoreRefs")
    //println(s"CRC: ${coreDoc.corefChains()}")
    coreDoc.corefChains().forEach( (k, v) =>
      println(s"CHAIN: ${v} \n")
    )

  }
}
