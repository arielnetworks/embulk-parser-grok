Embulk::JavaPlugin.register_parser(
  "grok", "org.embulk.parser.grok.GrokParserPlugin",
  File.expand_path('../../../../classpath', __FILE__))
