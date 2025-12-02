class MyDemo::Translator
  def initialize(language = "english")
    @language = language
  end

  def hi
    case @language
    when "spanish"
      "demo mundo"
    when "korean"
      "anyoung ha se yo"
    else
      "hello world"
    end
  end
end
