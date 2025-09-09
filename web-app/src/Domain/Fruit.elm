module Domain.Fruit exposing (..)

import Json.Decode exposing (Decoder, field, map2, string)
type alias Fruit =
  {   name : String
    , description: String
  }

fruitDecoder : Decoder Fruit
fruitDecoder =
  map2 Fruit
    (field "name" string)
    (field "description" string)

fruitsDecoder : Decoder (List Fruit)
fruitsDecoder =
    Json.Decode.list fruitDecoder

    -- First ever converter
fruitsToString: (List Fruit) -> (List String)
fruitsToString fruits =
        List.map (\f -> f.name ) fruits