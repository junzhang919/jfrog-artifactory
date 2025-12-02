class MyDemo
  def self.hi(language)
    translator = Translator.new(language)
    translator.hi
  end
end

require 'demo/translator'
