package tapl.extracted

import examples._
import util.Print._

@adts(Binding,Tm)
@ops(BindingShift, PBinding, GetTypeFromBind, Eval1, IsVal, PtmTerm, PtmATerm, PtmAppTerm, TmMap)
@family
trait Top extends Typed {
  @adt trait Ty extends super.Ty {
    case object TyTop
  }

  @default(Ty) trait PtyType extends super.PtyType
  @default(Ty) trait PtyArrowType extends super.PtyArrowType
  @visit(Ty) trait PtyAType extends super.PtyAType {
    def tyTop = (_,_) => "Top"
  }

  @visit(Tm) trait Typeof extends super.Typeof {
    override def tmApp = x => ctx => {
      val ty1 = this(x.t1)(ctx)
      val ty2 = this(x.t2)(ctx)
      ty1 match {
        case TyArr(ty11,ty12) =>
          if (subtype(ty2)(ty11)) ty12
          else throw new Exception("parameter mismatch" + " : " + ty2 + " != " + ty11)
        case _ => throw new Exception("arrow type expected")
      }
    }
  }

  @visit(Ty) trait Subtype extends super.Subtype with TyEqv {
    override def apply(ty1: Ty) = ty2 =>
      (ty1 == ty2) || (ty2 match {
        case TyTop => true
        case _ => try { ty1.accept(this)(ty2) }
                  catch { case _: MatchError => false }
      })
  }

  @visit(Ty) trait TyEqv extends super.TyEqv {
    def tyTop = { case TyTop => true } // BUG: a missing case in Ilya's impl
  }
}

@family
@adts(Binding,Tm,Ty)
@ops(BindingShift, PBinding, GetTypeFromBind, Eval1, IsVal, PtmTerm, PtmATerm, PtmAppTerm, TmMap, Typeof, PtyType, PtyAType, PtyArrowType, Subtype, TyEqv)
trait TopJoinMeet extends Top {
  @default(Ty) trait Meet {
    type OTy = Ty => Ty

    def ty = _ => _ => throw NoRuleApplies()
    def default: Ty = throw NoRuleApplies()
    override def apply(ty1: Ty) = ty2 => {
      if (subtype(ty1)(ty2)) {
        ty2
      } else if (subtype(ty2)(ty1)) {
        ty1
      } else {
        try { ty1.accept(this)(ty2) }
        catch { case _: MatchError => default }
      }
    }

    override def tyArr = x => {
      case TyArr(ty1,ty2) => TyArr(meet(x.ty1)(ty1), this(x.ty2)(ty2))
    }
  }

  @default(Ty) trait Join {
    type OTy = Ty => Ty
    def ty = _ => _ => throw new MatchError()
    override def apply(ty1: Ty) = ty2 => {
      if (subtype(ty1)(ty2)) {
        ty2
      } else if (subtype(ty2)(ty1)) {
        ty1
      } else {
        try { ty1.accept(this)(ty2) }
        catch { case _: MatchError => TyTop }
      }
    }
    override def tyArr = x => {
      case TyArr(ty1,ty2) => TyArr(join(x.ty1)(ty1), this(x.ty2)(ty2))
    }
  }
}

