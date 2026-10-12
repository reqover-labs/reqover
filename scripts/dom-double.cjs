// SPDX-License-Identifier: Apache-2.0
// Minimal controller doubles, not a browser/rendering replacement.
class Element {
  constructor(tag='div'){this.tag=tag;this.children=[];this.dataset={};this.style={};this.attributes={};this.handlers={};this.value='';this.files=[];this.hidden=false;this.className='';this.ownText='';this.classList={add:c=>{this.className+=' '+c;}};}
  append(...children){children.forEach(child=>{this.children.push(child);child.parentElement=this;});}
  replaceChildren(...children){this.children=[];this.append(...children);}
  set textContent(value){this.ownText=String(value);this.children=[];}
  get textContent(){return this.ownText+this.children.map(c=>c.textContent).join(' ');}
  get firstChild(){return this.children[0];}
  setAttribute(k,v){this.attributes[k]=String(v);}
  getAttribute(k){return this.attributes[k]||null;}
  addEventListener(type,handler){this.handlers[type]=handler;}
  fire(type){return this.handlers[type]?.({type});}
  click(){return this.fire('click');}
  focus(){}
  querySelectorAll(selector){return this.children.flatMap(c=>[(selector===c.tag || (selector.startsWith('.') && c.className.split(' ').includes(selector.slice(1))))?c:null,...c.querySelectorAll(selector)]).filter(Boolean);}
  querySelector(selector){return this.querySelectorAll(selector)[0];}
}
module.exports={Element};
