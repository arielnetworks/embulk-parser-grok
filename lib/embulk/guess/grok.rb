Embulk::JavaPlugin.register_guess(
  "grok", "org.embulk.parser.grok.GrokGuessPlugin",
  File.expand_path('../../../../classpath', __FILE__))
