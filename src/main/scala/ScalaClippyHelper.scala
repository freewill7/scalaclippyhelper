import java.io.{FileInputStream, StringWriter}
import java.text.SimpleDateFormat

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

import scala.io._
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.{FirebaseApp, FirebaseOptions}

import scala.beans.BeanProperty

class SchemaV1PlayerId {
  var playerId = ""
  var playerName = ""
}

@JsonIgnoreProperties(ignoreUnknown=true)
class SchemaV1CharacterPreferences {
  var name = ""
  var score = 1
}

@JsonIgnoreProperties(ignoreUnknown=true)
class SchemaV1Result {
  var date = ""
  var p1Character = ""
  var p2Character = ""
  var winnerId = ""
}

@JsonIgnoreProperties(ignoreUnknown=true)
class SchemaV1 {
  var players = Map[String,SchemaV1PlayerId]()
  val preferences = Map[String,Map[String,SchemaV1CharacterPreferences]]()
  val results = Map[String,SchemaV1Result]()
}

// Represents a character preference
// The @BeanProperty annotation is required for FirebaseDatabase serialisation
class FirebasePref(@BeanProperty val name : String, @BeanProperty val p1Rating : Int, @BeanProperty val p2Rating : Int ) {
}

// Represents a result
// The @BeanProperty annotation is required for FirebaseDatabase serialisation
class FirebaseResult( @BeanProperty val date: String,
                      @BeanProperty val p1Character: String,
                      @BeanProperty val p2Character : String,
                      @BeanProperty val p1Won : Boolean ) {
}

object Main extends App {

  def matchPlayerName(playerName: String)(value: (String,SchemaV1PlayerId)): Boolean = {
    return playerName == value._2.playerName
  }

  override def main(args : Array[String]): Unit = {
    if ( 2 != args.size ) {
      println( "usage: scalaclippyhelper [userid] [path]")
      return
    }

    val userId = args(0)
    val filename = args(1)

    // parse the previous results
    val json = Source.fromFile(filename)
    val mapper = new ObjectMapper() with ScalaObjectMapper
    mapper.registerModule(DefaultScalaModule)
    val parsedJson = mapper.readValue[SchemaV1](json.reader())

    // identify player ids
    val playerNames = List("Red Panda", "Blue Goose")
    val playerIds = playerNames.map( name =>
      parsedJson.players.find(matchPlayerName(name)).head._1 )
    println( "Red Panda => " + playerIds(0));
    println( "Blue Goose => " + playerIds(1));

    // identify old character identifiers
    val characterIds = parsedJson.preferences.map(pref=>pref._2.keySet)
      .foldLeft( Set[String]() )( (seta,setb) => seta.union(setb) )

    // construct new characters preserving player ratings
    val prefs = characterIds.map( id => {
      val prefDir = parsedJson.preferences
      val p1Pref = prefDir(playerIds(0))
      val p2Pref = prefDir(playerIds(1))
      val name = p1Pref(id).name
      val p1Score = p1Pref(id).score
      val p2Score = p2Pref(id).score
      new FirebasePref(name, p1Score, p2Score)
    } )

    // connect to firebase
    val serviceAccount = new FileInputStream("serviceAccount.json")
    val options = new FirebaseOptions.Builder()
      .setCredentials(GoogleCredentials.fromStream(serviceAccount))
      .setDatabaseUrl("https://sfclippy-83815.firebaseio.com/")
      .build()
    FirebaseApp.initializeApp(options)

    // work relative to the provided user directory
    val pathUserDir = "/users/" + userId
    val refUserDir = FirebaseDatabase.getInstance( FirebaseApp.getInstance() ).getReference( pathUserDir )

    // write characters to firebase and take note of generated ids
    val refCharacters = refUserDir.child("characters")
    val characterMappings = prefs.map( pref => {
      val charRef = refCharacters.push()
      charRef.setValueAsync( pref )
      (pref.name,charRef.getKey)
    }).toMap

    // write results to firebase using the new character ids
    val refResults = refUserDir.child("results")
    val oldDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val newDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
    parsedJson.results.values.foreach( oldResult => {
      val oldDate = oldResult.date
      val newDate = newDateFormat.format( oldDateFormat.parse(oldDate) )
      val p1CharId = characterMappings(oldResult.p1Character)
      val p2CharId = characterMappings(oldResult.p2Character)
      val p1Won = playerIds.head == oldResult.winnerId
      val result = new FirebaseResult( newDate, p1CharId, p2CharId, p1Won )
      val resultRef = refResults.push()
      resultRef.setValueAsync(result)
    } )
  }
}
