module FancySearch exposing (main)

import Basics as Int
import Browser
import Html exposing (Html, Attribute, button, div, input, text, ul, li, span)
import Html.Attributes exposing (..)
import Html.Events exposing (onInput, onBlur, onFocus, on)
import Json.Decode as Decode
import Http
import Task
import Process


-- MAIN


main : Program () Model Msg
main =
    Browser.element
        { init = \_ -> ( initModel, Cmd.none )
        , update = update
        , view = view
        , subscriptions = \_ -> Sub.none
        }



-- MODEL
type alias Suggestion =
    { id : Int
    , name : String
    }


type alias Model =
    { query : String
    , suggestions : List Suggestion
    , highlighted : Maybe Int -- index into suggestions
    , loading : Bool
    , debounceToken : Int -- increments each time query changes
    }


initModel : Model
initModel =
    { query = ""
    , suggestions = []
    , highlighted = Nothing
    , loading = False
    , debounceToken = 0
    }



-- MESSAGES


type Msg
    = InputChanged String
    | Debounced Int
    | FetchResults Int
    | GotResults Int (Result Http.Error (List Suggestion))
    | KeyDown String
    | SuggestionClicked Suggestion
    | ClearSuggestions
    | Focused
    | NoOp



-- UPDATE


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        InputChanged q ->
            let
                nextToken =
                    model.debounceToken + 1

                -- start debounce (400 ms) and pass token back
                debounceCmd =
                    Task.perform (\_ -> Debounced nextToken) (Process.sleep 400)
            in
            ( { model
                | query = q
                , debounceToken = nextToken
                , loading = if q == "" then False else True
                , highlighted = Nothing
              }
            , debounceCmd
            )

        Debounced token ->
            if token == model.debounceToken then
                if String.trim model.query == "" then
                    ( { model | suggestions = [], loading = False }, Cmd.none )
                else
                    -- send FetchResults token as an immediate Cmd using Task.perform
                    ( model, Task.perform (\_ -> FetchResults token) (Task.succeed ()) )
            else
                ( model, Cmd.none )

        FetchResults token ->
            ( model, fetchSuggestions token model.query )

        GotResults token result ->
            if token == model.debounceToken then
                case result of
                    Ok list ->
                        ( { model | suggestions = list, loading = False, highlighted = Nothing }, Cmd.none )

                    Err _ ->
                        -- On error, just clear suggestions and stop loading.
                        ( { model | suggestions = [], loading = False, highlighted = Nothing }, Cmd.none )
            else
                ( model, Cmd.none )

        KeyDown key ->
            case key of
                "ArrowDown" ->
                    let
                        next =
                            case model.highlighted of
                                Nothing ->
                                    if List.isEmpty model.suggestions then
                                        Nothing
                                    else
                                        Just 0

                                Just i ->
                                    let
                                        maxIndex =
                                            List.length model.suggestions - 1
                                    in
                                    Just (Int.min (i + 1) maxIndex)
                    in
                    ( { model | highlighted = next }, Cmd.none )

                "ArrowUp" ->
                    let
                        prev =
                            case model.highlighted of
                                Nothing ->
                                    Nothing

                                Just i ->
                                    if i <= 0 then
                                        Nothing
                                    else
                                        Just (i - 1)
                    in
                    ( { model | highlighted = prev }, Cmd.none )

                "Enter" ->
                    case model.highlighted of
                        Nothing ->
                            ( model, Cmd.none )

                        Just idx ->
                            case List.drop idx model.suggestions |> List.head of
                                Nothing ->
                                    ( model, Cmd.none )

                                Just s ->
                                    ( { model | query = s.name, suggestions = [], highlighted = Nothing }, Cmd.none )

                "Escape" ->
                    ( { model | suggestions = [], highlighted = Nothing }, Cmd.none )

                _ ->
                    ( model, Cmd.none )

        SuggestionClicked s ->
            ( { model | query = s.name, suggestions = [], highlighted = Nothing }, Cmd.none )

        ClearSuggestions ->
            ( { model | suggestions = [], highlighted = Nothing }, Cmd.none )

        Focused ->
            -- when focused, we might re-run a search for existing query
            if String.trim model.query == "" then
                ( model, Cmd.none )
            else
                ( model,  Task.perform (\_ -> FetchResults model.debounceToken) (Task.succeed ()) )

        NoOp ->
            ( model, Cmd.none )



-- HTTP / API


-- Replace with your real API. Example expected response:
-- [{ "id": 123, "name": "hello" }, { "id": 321, "name": "world" }]
apiUrl : String
apiUrl =
    "http://localhost:8080/api/suggest?q="


fetchSuggestions : Int -> String -> Cmd Msg
fetchSuggestions token q =
    let
        url =
            apiUrl ++ q
    in
    Http.get
        { url = url
        , expect = Http.expectJson (GotResults token) suggestionsDecoder
        }


suggestionDecoder : Decode.Decoder Suggestion
suggestionDecoder =
    Decode.map2 Suggestion
        (Decode.field "id" Decode.int)
        (Decode.field "name" Decode.string)


suggestionsDecoder : Decode.Decoder (List Suggestion)
suggestionsDecoder =
    Decode.list suggestionDecoder



-- VIEW


view : Model -> Html Msg
view model =
    div [ style "font-family" "system-ui, -apple-system, 'Segoe UI', Roboto, Helvetica", style "max-width" "420px" ]
        [ div [ style "position" "relative" ]
            [ input
                [ placeholder "Search..."
                , value model.query
                , onInput InputChanged
                , on "keydown" (Decode.map KeyDown keyDecoder)
                , onBlur ClearSuggestions
                , onFocus Focused
                , style "width" "100%"
                , style "padding" "10px 12px"
                , style "box-sizing" "border-box"
                , style "border-radius" "6px"
                , style "border" "1px solid #ccc"
                ]
                []
            , viewSpinner model.loading
            , viewSuggestions model
            ]
        ]


keyDecoder : Decode.Decoder String
keyDecoder =
    Decode.field "key" Decode.string


viewSpinner : Bool -> Html msg
viewSpinner loading =
    if loading then
        span
            [ style "position" "absolute"
            , style "right" "10px"
            , style "top" "10px"
            , style "font-size" "12px"
            , attribute "aria-hidden" "true"
            ]
            [ text "…loading" ]
    else
        text ""


viewSuggestions : Model -> Html Msg
viewSuggestions model =
    case model.suggestions of
        [] ->
            text ""

        list ->
            ul
                [ style "list-style" "none"
                , style "margin" "6px 0 0 0"
                , style "padding" "6px"
                , style "box-shadow" "0 6px 18px rgba(0,0,0,0.08)"
                , style "border-radius" "6px"
                , style "background" "white"
                , style "max-height" "260px"
                , style "overflow" "auto"
                , attribute "role" "listbox"
                ]
                (List.indexedMap (renderSuggestion model.highlighted) list)


renderSuggestion : Maybe Int -> Int -> Suggestion -> Html Msg
renderSuggestion highlighted idx s =
    let
        isActive =
            Maybe.map ((==) idx) highlighted |> Maybe.withDefault False

        baseStyle =
            [ style "padding" "8px 10px"
            , style "cursor" "pointer"
            , on "mousedown" (Decode.succeed (SuggestionClicked s)) -- mousedown so it fires before blur
            ]

        styleWithBg =
            if isActive then
                style "background" "#f0f6ff" :: baseStyle
            else
                baseStyle
    in
    li (styleWithBg ++ [ attribute "role" "option" ])
        [ div [] [ text s.name ] ]
